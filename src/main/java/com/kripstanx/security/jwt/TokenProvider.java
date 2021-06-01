package com.kripstanx.security.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.ObjectUtils;
import tech.jhipster.config.JHipsterProperties;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    public static final String AUTHORITIES_KEY = "auth";
    public static final String SALT_KEY = "salt";

    private final Key secretKey;

    public TokenProvider(JHipsterProperties jHipsterProperties) {

        secretKey = Keys.hmacShaKeyFor(jHipsterProperties
                .getSecurity()
                .getAuthentication()
                .getJwt()
                .getSecret()
                .getBytes(StandardCharsets.UTF_8));

        /* this.secretKey = Base64.getEncoder().encodeToString(
            jHipsterProperties
                .getSecurity()
                .getAuthentication()
                .getJwt()
                .getSecret()
                .getBytes(StandardCharsets.UTF_8)
        ); */
    }

    public String createToken(Authentication authentication, long tokenValidityInMilliseconds) {
        String authorities = authentication.getAuthorities().stream()
                                           .map(GrantedAuthority::getAuthority)
                                           .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + tokenValidityInMilliseconds);

        String token = Jwts.builder()
                           .setSubject(authentication.getName())
                           .claim(AUTHORITIES_KEY, authorities)
                           .claim(SALT_KEY, UUID.randomUUID().toString())
                           .signWith(secretKey, SignatureAlgorithm.HS512)
                           .setExpiration(validity)
                           .compact();
        return token;
    }

    public Optional<Claims> validateTokenAndGetClaims(String authToken) {
        try {
              Jws<Claims> claimsJws = Jwts
                      .parserBuilder()
                      .setSigningKey(secretKey)
                      .build()
                      .parseClaimsJws(authToken);

            /* Jws<Claims> claimsJws = Jwts
                .parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(authToken); */
            return Optional.of(claimsJws.getBody());
        } catch (JwtException | IllegalArgumentException e) {
            log.info("Invalid JWT token.");
            log.trace("Invalid JWT token trace.", e);
        }
        return Optional.empty();
    }
}
