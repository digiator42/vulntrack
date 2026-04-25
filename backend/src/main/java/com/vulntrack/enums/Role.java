package com.vulntrack.enums;

public enum Role {
    ROLE_DEVELOPER,      // Can view assigned issues, update status
    ROLE_SECURITY_ADMIN, // Can view/create/edit ALL issues, manage CVEs
    ROLE_ADMIN           // Superuser: Manage users, delete anything, global stats
}