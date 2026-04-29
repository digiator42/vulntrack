package com.vulntrack.utils;

import com.vulntrack.dto.SearchCriteria;
import com.vulntrack.model.Issue;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("unchecked")
public class IssueSpecification implements Specification<Issue> {
    private final SearchCriteria criteria;

    public IssueSpecification(SearchCriteria criteria) { this.criteria = criteria; }

    @Override
    public Predicate toPredicate(
        Root<Issue> root,
        CriteriaQuery<?> query,
        CriteriaBuilder builder
    ) {
        String key = criteria.key();
        String operation = criteria.operation();
        Object value = criteria.value();

        // Check if the field is an Enum dynamically
        if (isEnumField(root, key)) {
            try {
                // Dynamically convert String "HIGH" -> Severity.HIGH (or Ordinal)
                Enum<?> enumValue = EnumUtils.convertToEnum(Issue.class, key, value);

                if (operation.equalsIgnoreCase(":")) {
                    return builder.equal(root.get(key), enumValue);
                }

                // For Enums, ">" usually means "Higher Ordinal/Severity"
                if (operation.equalsIgnoreCase(">")) {
                    return builder.greaterThan(root.get(key), (Comparable) enumValue);
                }

                if (operation.equalsIgnoreCase("<")) {
                    return builder.lessThan(root.get(key), (Comparable) enumValue);
                }

            } catch (IllegalArgumentException e) {
                // Return a predicate that is always false if enum conversion fails
                return builder.disjunction();
            }
        }

        // Handle Standard String/Number Logic
        if (operation.equalsIgnoreCase(":")) {
            if (root.get(key).getJavaType() == String.class) {
                return builder.like(root.get(key), "%" + value + "%");
            } else {
                return builder.equal(root.get(key), value);
            }
        }

        if (operation.equalsIgnoreCase(">")) {
            return builder.greaterThanOrEqualTo(root.get(key), value.toString());
        }

        if (operation.equalsIgnoreCase("<")) {
            return builder.lessThanOrEqualTo(root.get(key), value.toString());
        }

        return null;
    }

    // Helper to check if a field is an Enum without throwing exceptions
    private boolean isEnumField(Root<?> root, String fieldName) {
        try {
            return root.get(fieldName).getJavaType().isEnum();
        } catch (Exception e) {
            return false;
        }
    }
}