package com.recruiting.platform.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * LOCAL DEVELOPMENT ONLY: signs every request in as one configured user (app.security.dev-auto-login-as), so the
 * login screen is skipped. Registered only when that property is set; never set it outside the local profile.
 */
class DevAutoLoginFilter extends OncePerRequestFilter {

    private final UserDetailsService users;
    private final String email;

    DevAutoLoginFilter(UserDetailsService users, String email) {
        this.users = users;
        this.email = email;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            UserDetails user = users.loadUserByUsername(email);
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        }
        chain.doFilter(request, response);
    }
}
