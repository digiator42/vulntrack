package com.vulntrack.utils;

import com.vulntrack.annotation.AuditLog;
import com.vulntrack.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {
    private final AuditLogService auditLogService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.vulntrack.annotation.AuditLog)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog audit = method.getAnnotation(AuditLog.class);

        String resolvedId = resolveSpEL(joinPoint, audit.id());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = auth != null ? auth.getName() : "anonymous";

        String status = "SUCCESS";
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            status = "FAILURE";
            throw e;
        } finally {
            auditLogService
                .persistAuditLog(principal, audit.action(), audit.resource(), resolvedId, status);
        }
    }

    private String resolveSpEL(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank())
            return "N/A";

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = nameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        context
            .setVariable("authentication", SecurityContextHolder.getContext().getAuthentication());

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            return parser.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            return "UNRESOLVED";
        }
    }
}