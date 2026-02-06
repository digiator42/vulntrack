package com.megastar.vulntrack.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class UserRepositoryImpl implements UserRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void performComplexSecurityAudit(Long userId) {
        System.out.println("Running custom audit for user: " + userId);
    }
}
