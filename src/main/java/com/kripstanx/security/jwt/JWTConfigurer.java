package com.kripstanx.security.jwt;

import com.kripstanx.security.SessionService;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JWTConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private SessionService sessionService;

    public JWTConfigurer(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        JWTFilter customFilter = new JWTFilter(sessionService);
        http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
