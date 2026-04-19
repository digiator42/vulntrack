package com.vulntrack.dto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record UnifiedErrorResponse(
                String message,
                List<ValidationError> errors,
                int code,
                String status) {
        public static ResponseEntity<UnifiedErrorResponse> res(
                        String msg,
                        List<ValidationError> errs,
                        HttpStatus status) {

                HttpStatus resolvedStatus = (status != null) ? status : HttpStatus.BAD_REQUEST;

                UnifiedErrorResponse body = new UnifiedErrorResponse(
                                msg,
                                errs,
                                resolvedStatus.value(),
                                resolvedStatus.name());

                return new ResponseEntity<>(body, resolvedStatus);
        }
}