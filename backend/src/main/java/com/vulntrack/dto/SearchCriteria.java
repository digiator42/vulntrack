
package com.vulntrack.dto;

public record SearchCriteria(

    String key, // The field name (severity or title)
    String operation, // The operator (:, >, <)
    Object value // The value to filter by
) {
}
