package com.megastar.vulntrack.dto;

import java.util.List;

public record ValidationErrorResponse(List<ValidationError> errors) {}
