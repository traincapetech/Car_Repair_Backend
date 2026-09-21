package com.carservice.backend.user.enums;

import java.util.*;

public final class RolePermissions {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS_MAP = new EnumMap<>(UserRole.class);

    static {
        // 1. SUPER_ADMIN has ALL permissions across every module
        ROLE_PERMISSIONS_MAP.put(UserRole.SUPER_ADMIN, EnumSet.allOf(Permission.class));

        // 2. ADMIN has all operational administrative permissions
        ROLE_PERMISSIONS_MAP.put(UserRole.ADMIN, EnumSet.of(
                Permission.USER_VIEW,
                Permission.USER_UPDATE,
                Permission.USER_STATUS_CHANGE,
                Permission.CUSTOMER_VIEW,
                Permission.CUSTOMER_STATUS_CHANGE,
                Permission.WORKSHOP_VIEW,
                Permission.WORKSHOP_APPROVE,
                Permission.WORKSHOP_REJECT,
                Permission.WORKSHOP_SUSPEND,
                Permission.WORKSHOP_REACTIVATE,
                Permission.SERVICE_VIEW,
                Permission.SERVICE_CREATE,
                Permission.SERVICE_UPDATE,
                Permission.SERVICE_DELETE,
                Permission.BOOKING_VIEW,
                Permission.BOOKING_UPDATE,
                Permission.PAYMENT_VIEW,
                Permission.PAYMENT_MANAGE,
                Permission.AUDIT_VIEW,
                Permission.SYSTEM_SETTINGS_VIEW,
                Permission.SYSTEM_SETTINGS_UPDATE
        ));

        // 3. OPERATIONS_ADMIN focuses on dispatch, workshops, catalog, and bookings
        ROLE_PERMISSIONS_MAP.put(UserRole.OPERATIONS_ADMIN, EnumSet.of(
                Permission.USER_VIEW,
                Permission.CUSTOMER_VIEW,
                Permission.WORKSHOP_VIEW,
                Permission.WORKSHOP_APPROVE,
                Permission.WORKSHOP_SUSPEND,
                Permission.WORKSHOP_REACTIVATE,
                Permission.SERVICE_VIEW,
                Permission.BOOKING_VIEW,
                Permission.BOOKING_UPDATE,
                Permission.SYSTEM_SETTINGS_VIEW
        ));

        // 4. FINANCE_ADMIN focuses on payments, wallet operations, refunds, and audits
        ROLE_PERMISSIONS_MAP.put(UserRole.FINANCE_ADMIN, EnumSet.of(
                Permission.USER_VIEW,
                Permission.WORKSHOP_VIEW,
                Permission.BOOKING_VIEW,
                Permission.PAYMENT_VIEW,
                Permission.PAYMENT_MANAGE,
                Permission.AUDIT_VIEW
        ));

        // 5. SUPPORT_AGENT handles customer care, view workshops, and manage customer tickets
        ROLE_PERMISSIONS_MAP.put(UserRole.SUPPORT_AGENT, EnumSet.of(
                Permission.USER_VIEW,
                Permission.CUSTOMER_VIEW,
                Permission.CUSTOMER_STATUS_CHANGE,
                Permission.WORKSHOP_VIEW,
                Permission.SERVICE_VIEW,
                Permission.BOOKING_VIEW,
                Permission.BOOKING_UPDATE
        ));

        // 6. WORKSHOP_OWNER & PARTNER (alias)
        Set<Permission> workshopOwnerPerms = EnumSet.of(
                Permission.WORKSHOP_VIEW,
                Permission.SERVICE_VIEW,
                Permission.BOOKING_VIEW,
                Permission.PAYMENT_VIEW
        );
        ROLE_PERMISSIONS_MAP.put(UserRole.WORKSHOP_OWNER, workshopOwnerPerms);
        ROLE_PERMISSIONS_MAP.put(UserRole.PARTNER, workshopOwnerPerms);

        // 7. WORKSHOP_STAFF
        ROLE_PERMISSIONS_MAP.put(UserRole.WORKSHOP_STAFF, EnumSet.of(
                Permission.SERVICE_VIEW,
                Permission.BOOKING_VIEW
        ));

        // 8. CUSTOMER
        ROLE_PERMISSIONS_MAP.put(UserRole.CUSTOMER, EnumSet.of(
                Permission.SERVICE_VIEW,
                Permission.BOOKING_VIEW
        ));
    }

    private RolePermissions() {
    }

    public static Set<Permission> getPermissionsForRole(UserRole role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return ROLE_PERMISSIONS_MAP.getOrDefault(role, Collections.emptySet());
    }

    public static boolean hasPermission(UserRole role, Permission permission) {
        return getPermissionsForRole(role).contains(permission);
    }
}
