package com.campusnexus.security;

import com.campusnexus.common.exception.ErrorCode;
import com.campusnexus.common.result.Result;
import com.campusnexus.entity.User;
import com.campusnexus.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        JwtPayload payload;
        try {
            payload = jwtService.parse(token);
        } catch (ExpiredJwtException ex) {
            writeError(response, ErrorCode.TOKEN_EXPIRED, "Token 已过期");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            writeError(response, ErrorCode.TOKEN_INVALID, "Token 无效");
            return;
        }

        if (tokenBlacklistService.isBlacklisted(payload.jti())) {
            writeError(response, ErrorCode.TOKEN_INVALID, "Token 已失效");
            return;
        }

        User user = userMapper.selectById(payload.userId());
        if (user == null) {
            writeError(response, ErrorCode.USER_NOT_FOUND, "用户不存在");
            return;
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            writeError(response, ErrorCode.ACCOUNT_DISABLED, "账号已禁用");
            return;
        }

        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (currentVersion != payload.tokenVersion()) {
            writeError(response, ErrorCode.TOKEN_INVALID, "Token 已失效");
            return;
        }

        LoginUser principal = new LoginUser(user.getId(), user.getUsername(), user.getRole(), currentVersion);
        List<GrantedAuthority> authorities = user.getRole() != null && user.getRole() == 1
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(errorCode.getCode(), message));
    }
}
