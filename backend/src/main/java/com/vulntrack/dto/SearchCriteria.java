
package com.vulntrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {
    private String key;       // The field name (severity or title)
    private String operation; // The operator (:, >, <)
    private Object value;     // The value to filter by
}
