package com.vulntrack.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action();          // "ACCESS_ISSUE", "UPDATE_STATUS"
    String resource();        // "ISSUE", "USER"
    String id() default "";   // SpEL expression, "#id", "#request.issueId"
}