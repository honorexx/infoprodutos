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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Resposta padronizada (Problem Details) para requisições não autenticadas
 * que tentam acessar recurso protegido. Nunca expõe detalhes internos.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorResponse body = ErrorResponse.of(
                "unauthenticated",
                "Não autenticado",
                HttpStatus.UNAUTHORIZED.value(),
                "É necessário autenticar-se para acessar este recurso.",
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
