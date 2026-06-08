package com.personalblog.ragbackend.common.web.handler;

import com.personalblog.ragbackend.common.web.domain.Result;
import com.personalblog.ragbackend.common.web.domain.Results;
import com.personalblog.ragbackend.framework.exception.ObjectStorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = "请求校验失败";
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        return jsonBody(HttpStatus.BAD_REQUEST, Results.failure("400", message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        String message = "请求校验失败";
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        return jsonBody(HttpStatus.BAD_REQUEST, Results.failure("400", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return jsonBody(HttpStatus.BAD_REQUEST, Results.failure("400", exception.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return jsonBody(HttpStatus.BAD_REQUEST, Results.failure("400", "缺少请求参数: " + exception.getParameterName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return jsonBody(HttpStatus.BAD_REQUEST, Results.failure("400", exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String code = String.valueOf(status.value());
        log.warn("ResponseStatusException: status={}, message={}", status.value(), message);
        return jsonBody(status, Results.failure(code, message));
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<Result<Void>> handleDatabaseUnavailable(Exception exception, HttpServletRequest request) {
        log.error(
                "Database unavailable while handling {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );
        return jsonBody(
                HttpStatus.SERVICE_UNAVAILABLE,
                Results.failure("503", "数据库暂时不可用，请检查 PostgreSQL 是否已经启动")
        );
    }

    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<Result<Void>> handleObjectStorageException(ObjectStorageException exception, HttpServletRequest request) {
        log.error(
                "Object storage error while handling {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );
        return jsonBody(
                HttpStatus.SERVICE_UNAVAILABLE,
                Results.failure("503", "对象存储暂时不可用，请检查 RustFS/S3 配置")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error(
                "Unhandled exception while handling {} {}: {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );
        return jsonBody(HttpStatus.INTERNAL_SERVER_ERROR, Results.failure());
    }

    private ResponseEntity<Result<Void>> jsonBody(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
