package com.kripstanx.security;

import com.kripstanx.domain.Authority;
import com.kripstanx.repository.UserRepository;
import com.kripstanx.security.jwt.TokenProvider;
import tech.jhipster.config.JHipsterProperties;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final long tokenValidityInMilliseconds;

    @Value("${application.allow-multiple-sessions:false}")
    private boolean allowMultipleSessions;

    public SessionService(JHipsterProperties jHipsterProperties,
                          TokenProvider tokenProvider,
                          UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.tokenValidityInMilliseconds =
            1000 * jHipsterProperties
                .getSecurity()
                .getAuthentication()
                .getJwt()
                .getTokenValidityInSeconds();
    }

    /**
     * @return JWT token for the session
     */
    public String createAndStoreSessionToken(Authentication authentication) {
        String token = tokenProvider.createToken(authentication, this.tokenValidityInMilliseconds);
        storeUserToken(authentication.getName(), token);
        return token;
    }

    @Transactional
    @Scheduled(cron = "0 * * * * *") // every minute
    public void removeExpiredTemporaryJwtTokens() {
        userRepository.clearTemporalJwtTokens(Instant.now());
    }

    public Optional<Claims> isSessionValid(String authToken) {
        Optional<Claims> maybeClaims = tokenProvider.validateTokenAndGetClaims(authToken);
        if (!maybeClaims.isPresent()) {
            return Optional.empty();
        }
        String username = maybeClaims.get().getSubject();
        Pair<String, String> expectedTokens = findUserToken(username);
        if (
            allowMultipleSessions || Objects.equals(expectedTokens.getLeft(), authToken) ||
                Objects.equals(expectedTokens.getRight(), authToken)
        ) {
            return maybeClaims;
        } else {
            return Optional.empty();
        }
    }

    public Authentication createAuthentication(String token, Claims claims) {
        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(claims.get(TokenProvider.AUTHORITIES_KEY).toString().split(","))
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public void invalidateSession(String username) {
        storeUserToken(username, null);
    }

    private void storeUserToken(String username, String token) {
        Optional<com.kripstanx.domain.User> optionalUser = userRepository.findOneByUsername(username);
        if (optionalUser.isPresent()) {
            com.kripstanx.domain.User user = optionalUser.get();
            if (token == null) {
                user.setJwtToken2(null);
                user.setJwtToken2ExpiresAt(null);
            } else {
                user.setJwtToken2(user.getJwtToken());
                user.setJwtToken2ExpiresAt(Instant.now().plusSeconds(60));
            }
            user.setJwtToken(token);
            userRepository.save(user);
        } else {
            throw new NoSuchElementException(username);
        }
    }

    private Pair<String, String> findUserToken(String username) {
        Optional<com.kripstanx.domain.User> optionalUser = userRepository.findOneByUsername(username);
        if (optionalUser.isPresent()) {
            com.kripstanx.domain.User user = optionalUser.get();
            return Pair.of(user.getJwtToken(), user.getJwtToken2());
        } else {
            throw new NoSuchElementException(username);
        }
    }

    private void lockOutNonTechnicalUsers() {
        userRepository.findAllWithAuthoritiesByJwtTokenNotNull()
                      .stream()
                      .filter(userDto -> !userDto.getAuthorities()
                                                 .stream()
                                                 .map(Authority::getName)
                                                 .anyMatch(s -> s.equals(AuthoritiesConstants.TECHNICAL)))
                      .forEach(user -> invalidateSession(user.getUsername()));
    }

}
