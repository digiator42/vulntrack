package com.vulntrack.exception;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vulntrack.dto.UnifiedErrorResponse;
import com.vulntrack.dto.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * JSON parsing HttpMessageNotReadableException → 400 (Invalid payload)
 *
 * Bean validation MethodArgumentNotValidException → 400 (errors[])
 *
 * Business rules DuplicateItemException → 409
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private HttpServletRequest request;

    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorized(UnauthorizedException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public Object handleForbidden(ForbiddenException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleForbidden(AccessDeniedException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadErrorRequestException.class)
    public Object handleBadRequest(BadErrorRequestException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object validationErrorsResponse(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(
                error -> new ValidationError(
                    error.getField(),
                    error.getDefaultMessage()
                )
            )
            .toList();

        return UnifiedErrorResponse.res(null, errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateItemException.class)
    public Object errorResponse(DuplicateItemException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleInvalidJson(HttpMessageNotReadableException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IssueNotFoundException.class)
    public Object handleIssueNotFound(IssueNotFoundException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public Object handleUsernameNotFound(org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
        return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }
}
