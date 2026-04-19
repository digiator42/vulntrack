package com.vulntrack.dto;

import java.util.List;

public record ValidationErrorResponse(List<ValidationError> errors) {
}
