package com.carservice.backend.user.enums;

public enum Permission {
    // User & Identity Permissions
    USER_VIEW("Users", "View platform user directory and user accounts"),
    USER_UPDATE("Users", "Update user profile and contact information"),
    USER_STATUS_CHANGE("Users", "Activate, deactivate, suspend or restore user accounts"),
    USER_ROLE_CHANGE("Users", "Assign and change user roles (Super Admin only)"),

    // Customer Permissions
    CUSTOMER_VIEW("Customers", "View customer records, vehicle garages and service histories"),
    CUSTOMER_STATUS_CHANGE("Customers", "Change customer account active status"),

    // Workshop Permissions
    WORKSHOP_VIEW("Workshops", "View workshop directory, facility profiles and operating metrics"),
    WORKSHOP_APPROVE("Workshops", "Approve pending workshop registrations into active network"),
    WORKSHOP_REJECT("Workshops", "Reject workshop applications with recorded reason"),
    WORKSHOP_SUSPEND("Workshops", "Suspend active workshop operations"),
    WORKSHOP_REACTIVATE("Workshops", "Reactivate suspended workshops"),

    // Service Catalog Permissions
    SERVICE_VIEW("Service Catalog", "View available platform service packages and pricing"),
    SERVICE_CREATE("Service Catalog", "Create new service packages in central catalog"),
    SERVICE_UPDATE("Service Catalog", "Update service package details, pricing and discounts"),
    SERVICE_DELETE("Service Catalog", "Deactivate or remove service packages from catalog"),

    // Bookings & Dispatch Permissions
    BOOKING_VIEW("Bookings", "View customer service bookings and marketplace service requests"),
    BOOKING_UPDATE("Bookings", "Update booking schedules, assign workshops and manage status"),

    // Payments & Wallets Permissions
    PAYMENT_VIEW("Payments", "View payment transactions, workshop wallet balances and settlement logs"),
    PAYMENT_MANAGE("Payments", "Manage wallet adjustments and process refund requests"),

    // Audit & Governance Permissions
    AUDIT_VIEW("Audit Logs", "Inspect immutable audit events and administrative mutation logs"),

    // System Settings Permissions
    SYSTEM_SETTINGS_VIEW("System Settings", "View platform diagnostic health and runtime settings"),
    SYSTEM_SETTINGS_UPDATE("System Settings", "Modify operational dispatch rules and platform parameters");

    private final String module;
    private final String description;

    Permission(String module, String description) {
        this.module = module;
        this.description = description;
    }

    public String getModule() {
        return module;
    }

    public String getDescription() {
        return description;
    }
}
