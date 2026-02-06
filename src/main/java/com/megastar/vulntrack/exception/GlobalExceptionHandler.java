package com.megastar.vulntrack.exception;

import com.megastar.vulntrack.dto.ErrorResponse;
import com.megastar.vulntrack.dto.UnauthorizedError;
import com.megastar.vulntrack.dto.ValidationError;
import com.megastar.vulntrack.dto.ValidationErrorResponse;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * JSON parsing
 * HttpMessageNotReadableException → 400 (Invalid payload)
 *
 * Bean validation
 * MethodArgumentNotValidException → 400 (errors[])
 *
 * Business rules
 * DuplicateIssueException → 409
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public UnauthorizedError handleUnauthorized(UnauthorizedException ex) {
        return new UnauthorizedError(ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public UnauthorizedError handleForbidden(ForbiddenException ex) {
        return new UnauthorizedError(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public UnauthorizedError handleForbidden(AccessDeniedException ex) {
        return new UnauthorizedError(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse validationErrorsResponse(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();
        return new ValidationErrorResponse(errors);
    }

    @ExceptionHandler(DuplicateIssueException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409 errors
    public ErrorResponse errorResponse(DuplicateIssueException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidJson(HttpMessageNotReadableException ex) {
        return new ErrorResponse("Invalid request payload -> " + ex.getMessage());
    }

    @ExceptionHandler(IssueNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIssueNotFound(IssueNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEmptyBodyPatch(BadRequestException ex) {
        return new ErrorResponse(ex.getMessage());
    }

}
