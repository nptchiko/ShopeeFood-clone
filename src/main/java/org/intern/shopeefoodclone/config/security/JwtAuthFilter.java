package org.intern.shopeefoodclone.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.intern.shopeefoodclone.shared.api.ErrorApiResponse;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Global Exception Handler can't catch exceptions thrown in filters, so we need to handle them here

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // here is where we catch exception manually
        try {

            jwtService.validateToken(token, true);



        } catch (AppException appException) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(String.valueOf(MediaType.APPLICATION_JSON));

            var resp = ErrorApiResponse.builder()
                    .message(appException.getMessage())
                    .detailMessage(appException.getErrorCode().getDetailedMessage())
                    .build();
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(resp));
            response.flushBuffer();

            // Don't continue the filter chain after writing the error response
            return;
        }

        String userId = jwtService.extractUserId(token);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null,
                List.of());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

}
