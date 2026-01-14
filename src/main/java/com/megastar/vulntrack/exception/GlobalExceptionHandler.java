package com.megastar.vulntrack.exception;

import com.megastar.vulntrack.dto.ErrorResponse;
import com.megastar.vulntrack.dto.ValidationError;
import com.megastar.vulntrack.dto.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * JSON parsing
 *  HttpMessageNotReadableException → 400 (Invalid payload)
 *
 * Bean validation
 *  MethodArgumentNotValidException → 400 (errors[])
 *
 * Business rules
 *  DuplicateIssueException → 409
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse validationErrorsResponse(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                )).toList();
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

}
