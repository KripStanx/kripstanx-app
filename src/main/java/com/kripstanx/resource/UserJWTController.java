package com.kripstanx.resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kripstanx.domain.User;
import com.kripstanx.resource.vm.LoginVM;
import com.kripstanx.security.SessionService;
import com.kripstanx.security.jwt.JWTConfigurer;
import com.kripstanx.security.jwt.JWTFilter;
import com.kripstanx.service.AuditEventService;
import com.kripstanx.service.UserService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final Logger log = LoggerFactory.getLogger(UserJWTController.class);

    private static final String EVENT_NAME_IN_CASE_OF_FAILED_LOGIN = "AUTHENTICATION_FAILURE";

    private final SessionService sessionService;

    private final AuthenticationManager authenticationManager;

    private final AuditEventService auditEventService;

    private final UserService userService;

    @Value("${application.login-inactivity-period-in-days:30}")
    private int loginInactivityPeriodInDays;

    public UserJWTController(SessionService sessionService,
                             AuthenticationManager authenticationManager,
                             AuditEventService auditEventService, UserService userService) {
        this.sessionService = sessionService;
        this.authenticationManager = authenticationManager;
        this.auditEventService = auditEventService;
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    @Timed
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletRequest request) {

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginVM.getUsername(),
                                                                                                          loginVM.getPassword());

        authenticationToken.setDetails(new WebAuthenticationDetails(request));

        try {
            final Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            if (lockUserIfNeededDueToInactivity(loginVM.getUsername())) {
                throw new DisabledException("User " + loginVM.getUsername() + " is disabled.");
            }

            return respondWithJwtTokenOrNavigateToResetScreen(loginVM, authentication);
        } catch (InternalAuthenticationServiceException internalAuthenticationServiceException) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                                 .header("message", internalAuthenticationServiceException.getMessage())
                                 .build();
        } catch (AuthenticationException authException) {
            lockUserIfExceedsThreeFailedAttempt(loginVM.getUsername());
            throw authException;
        }
    }

    private boolean lockUserIfNeededDueToInactivity(String login) {
        List<AuditEvent> lastSuccessfulLogins = auditEventService
            .getSuccesfulAuditEventsByUserOrderByAuditEventDateDesc(login);
        if (lastSuccessfulLogins.size() < 2) {
            return false;
        }

        // the first entry is this current login, so we need the second one
        Instant lastLoginTime = lastSuccessfulLogins.get(1).getTimestamp();
        if (lastLoginTime.isBefore(Instant.now().minus(Duration.ofDays(loginInactivityPeriodInDays)))) {
            log.info("User '{}' was locked due to inactivity. Last login time: {}", login, lastLoginTime);
            userService.lockActiveUser(login);
            return true;
        } else {
            return false;
        }
    }

    @PostMapping("/renew-token")
    @Timed
    public ResponseEntity<JWTToken> createNewToken(HttpServletRequest request) {
        String jwtToken = JWTFilter.resolveToken(request);
        Optional<Claims> maybeJwtClaims = this.sessionService.isSessionValid(jwtToken);
        Authentication authentication = sessionService.createAuthentication(jwtToken, maybeJwtClaims.get());
        String jwt = sessionService.createAndStoreSessionToken(authentication);
        return ResponseEntity.ok()
                             .header(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt)
                             .body(new JWTToken(jwt));
    }


    @GetMapping("/keep-alive-session")
    public Boolean keepAlive() {
        return true;
    }

    private ResponseEntity<JWTToken> respondWithJwtTokenOrNavigateToResetScreen(
            @RequestBody @Valid LoginVM loginVM, Authentication authentication) {
        String lowerCase = loginVM.getUsername().toLowerCase();
        if (userService.isUserPasswordExpired(lowerCase)) {
            String resetKey = requestResetIfUserPasswordExpired(lowerCase);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .header(HttpHeaders.LOCATION, "/reset/finish")
                                 .header("fdm-reset-key", resetKey)
                                 .build();
        } else {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            sessionService.invalidateSession(loginVM.getUsername());
            String jwt = sessionService.createAndStoreSessionToken(authentication);
            return ResponseEntity.ok()
                                 .header(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt)
                                 .body(new JWTToken(jwt));
        }
    }

    private void lockUserIfExceedsThreeFailedAttempt(String login) {
        List<AuditEvent> listOfLastThreeLoginAttempts = auditEventService.findTop3ByPrincipalOrderByAuditEventDateDesc(
            login);

        boolean maxFailedLoginAttempts =
            listOfLastThreeLoginAttempts.size() >= 3 && listOfLastThreeLoginAttempts.stream()
                                                                                    .allMatch(auditEvent ->
                                                                                                  EVENT_NAME_IN_CASE_OF_FAILED_LOGIN
                                                                                                      .equals(auditEvent
                                                                                                                  .getType()));

        if (maxFailedLoginAttempts) {
            userService.lockActiveUser(login);
        }
    }

    private String requestResetIfUserPasswordExpired(String login) {
        Optional<User> currentUser = userService.findOneByLogin(login);
        if (currentUser.isPresent()) {
            Optional<User> currentUserModified = userService.requestPasswordReset(currentUser.get().getEmail());
            if (currentUserModified.isPresent()) {
                return currentUserModified.get().getResetKey();
            }
        }
        return null;
    }

    @PostMapping("/logout")
    @Timed
    public ResponseEntity<Void> logout() {
        Optional<User> user = userService.getUserWithAuthorities();
        if (user.isPresent()) {
            User sessionUser = user.get();
            sessionService.invalidateSession(sessionUser.getUsername());
            log.debug("User with login {} has logged out", sessionUser.getUsername());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
