package org.kiwi.console.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kiwi.console.kiwi.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticateFilter extends OncePerRequestFilter {

    private final UserClient userClient;
    private final SysUserClient sysUserClient;

    public AuthenticateFilter(UserClient userClient, SysUserClient sysUserClient) {
        this.userClient = userClient;
        this.sysUserClient = sysUserClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        var auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        var token = auth.substring(7);
        var sysUserId = sysUserClient.authenticate(new SysAuthenticateRequest(token));
        if (sysUserId != null) {
            var userId = userClient.getBySysUserId(new GetBySysUserIdRequest(sysUserId));
            var authToken = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(request, response);
    }

}
