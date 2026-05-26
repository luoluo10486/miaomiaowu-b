package com.personalblog.ragbackend.common.web.handler;

import com.personalblog.ragbackend.common.web.domain.Result;
import com.personalblog.ragbackend.common.web.domain.Results;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = "请求参数校验失败";
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        return Results.failure("400", message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = "请求参数校验失败";
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        return Results.failure("400", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Results.failure("400", exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException exception) {
        return Results.failure("400", "缺少请求参数：" + exception.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return Results.failure("400", exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Result<Void> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        String code = String.valueOf(exception.getStatusCode().value());
        return Results.failure(code, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Results.failure();
    }
}
