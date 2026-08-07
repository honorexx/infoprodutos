package com.infoprodutos.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.common.web.CorrelationIdFilter;
import com.infoprodutos.api.common.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorResponse body = ErrorResponse.of(
                "access-denied",
                "Acesso negado",
                HttpStatus.FORBIDDEN.value(),
                "Você não tem permissão para executar esta ação.",
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
