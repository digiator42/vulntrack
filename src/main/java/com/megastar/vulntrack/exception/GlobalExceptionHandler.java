package com.megastar.vulntrack.exception;

import com.megastar.vulntrack.dto.ErrorResponse;
import com.megastar.vulntrack.dto.UnauthorizedError;
import com.megastar.vulntrack.dto.ValidationError;
import com.megastar.vulntrack.dto.ValidationErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.net.http.HttpRequest;
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
@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private HttpServletRequest request;

    private boolean isApiRequest() {
        return request.getRequestURI().startsWith("/api");
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Object handleUnauthorized(UnauthorizedException ex) {
        if (isApiRequest()) {
            return new UnauthorizedError(ex.getMessage());
        }

        return "redirect:/web/login/error=" + ex.getMessage();
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
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(BadErrorRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadErrorRequestException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object validationErrorsResponse(MethodArgumentNotValidException ex) {
        if (isApiRequest()) {
            List<ValidationError> errors = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> new ValidationError(
                            error.getField(),
                            error.getDefaultMessage()))
                    .toList();
            return new ValidationErrorResponse(errors);
        }
        return "redirect:/issue-form";
    }

    @ExceptionHandler(DuplicateIssueException.class)
    public Object errorResponse(DuplicateIssueException ex) {
        if (isApiRequest()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("issue-form");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
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
}
