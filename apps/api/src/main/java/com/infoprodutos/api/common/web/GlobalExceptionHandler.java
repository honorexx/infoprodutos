package com.infoprodutos.api.common.web;

import com.infoprodutos.api.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ponto único de tratamento de erros da API. Toda resposta de erro segue o
 * formato padronizado {@link ErrorResponse}. Stack traces e detalhes internos
 * nunca são expostos ao cliente (docs/SECURITY.md secao 6).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Erro de negócio tratado: {} - {}", ex.getErrorType(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorType(), ex.getStatus().getReasonPhrase(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = ErrorResponse.of(
                "validation-error",
                "Erro de validação",
                HttpStatus.BAD_REQUEST.value(),
                "Um ou mais campos são inválidos.",
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "malformed-request",
                "Requisição inválida",
                "O corpo da requisição não pôde ser interpretado.",
                request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(
                HttpStatus.UNAUTHORIZED,
                "invalid-credentials",
                "Credenciais inválidas",
                "E-mail ou senha incorretos.",
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(
                HttpStatus.UNAUTHORIZED,
                "unauthenticated",
                "Não autenticado",
                "É necessário autenticar-se para acessar este recurso.",
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Acesso negado",
                "Você não tem permissão para executar esta ação.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        log.error("Erro inesperado [correlationId={}]", correlationId, ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Erro interno",
                "Ocorreu um erro inesperado. Se o problema persistir, informe o identificador de correlação ao suporte.",
                request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String type, String title, String detail, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                type, title, status.value(), detail, request.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY));
        return ResponseEntity.status(status).body(body);
    }
}
