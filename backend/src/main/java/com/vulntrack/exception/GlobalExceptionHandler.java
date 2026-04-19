package com.vulntrack.exception;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.vulntrack.dto.ModelViewResponse;
import com.vulntrack.dto.UnifiedErrorResponse;
import com.vulntrack.dto.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

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
    public Object handleUnauthorized(UnauthorizedException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.FORBIDDEN, "login");
    }

    @ExceptionHandler(ForbiddenException.class)
    public Object handleForbidden(ForbiddenException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.FORBIDDEN, "issues");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleForbidden(AccessDeniedException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.FORBIDDEN, "issues");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.FORBIDDEN);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.FORBIDDEN, "issues");
    }

    @ExceptionHandler(BadErrorRequestException.class)
    public Object handleBadRequest(BadErrorRequestException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.BAD_REQUEST, "issues");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object validationErrorsResponse(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        if (isApiRequest()) {
            return UnifiedErrorResponse.res(null, errors, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = ModelViewResponse.res("Please fix the errors below", HttpStatus.BAD_REQUEST, "dashboard");
        mav.addObject("errors", errors);
        return "redirect:/web/issues?error=true";
    }

    @ExceptionHandler(DuplicateIssueException.class)
    public Object errorResponse(DuplicateIssueException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.CONFLICT);
        }

        return ModelViewResponse.res(ex.getMessage(), HttpStatus.NOT_FOUND, "issue-form");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleInvalidJson(HttpMessageNotReadableException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
        }

        return ModelViewResponse.res("Invalid request payload -> " + ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                "issues");
    }

    @ExceptionHandler(IssueNotFoundException.class)
    public Object handleIssueNotFound(IssueNotFoundException ex) {
        if (isApiRequest()) {
            return UnifiedErrorResponse.res(ex.getMessage(), null, HttpStatus.NOT_FOUND);
        }
        return ModelViewResponse.res(ex.getMessage(), HttpStatus.NOT_FOUND, "issues");
    }
}
