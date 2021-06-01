package com.kripstanx.security.jwt;

import com.kripstanx.security.AuthoritiesConstants;
import com.kripstanx.security.SessionService;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {

    private SessionService sessionService;

    public JWTFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwtToken = resolveToken(httpServletRequest);
        Optional<Claims> maybeJwtClaims = this.sessionService.isSessionValid(jwtToken);
        if (StringUtils.hasText(jwtToken) && maybeJwtClaims.isPresent()) {
            Authentication authentication = sessionService.createAuthentication(jwtToken, maybeJwtClaims.get());
            // in offline mode only technical users are allowed to send non-GET requests, except the POST(renew-token)
            boolean prohibited =
                (
                    !"GET".equalsIgnoreCase(((HttpServletRequest) servletRequest).getMethod()) &&
                        !httpServletRequest.getRequestURI().contains("renew-token")
                )
                    && isUserNotTechnical(authentication);
            if (prohibited) {
                ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                                                  "KripStanx is in offline mode");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isUserNotTechnical(Authentication authentication) {
        return authentication.getAuthorities()
                             .stream()
                             .noneMatch(o -> o.getAuthority().equals(
                                 AuthoritiesConstants.TECHNICAL));
    }

    public static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JWTConfigurer.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
