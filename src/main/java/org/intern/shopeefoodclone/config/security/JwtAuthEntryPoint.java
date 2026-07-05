package org.intern.shopeefoodclone.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.shared.api.ErrorApiResponse;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;


@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.error("ERROR: {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(String.valueOf(MediaType.APPLICATION_JSON));

        var resp = ErrorApiResponse.builder()
                .message("Unauthorized access")
                .detailMessage(ErrorCode.UNAUTHENTICATED.getDetailedMessage())
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(resp));
        response.flushBuffer();
    }
}
