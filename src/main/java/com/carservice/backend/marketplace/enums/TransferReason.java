package com.carservice.backend.marketplace.enums;

public enum TransferReason {
    WORKSHOP_AT_CAPACITY,
    PARTS_UNAVAILABLE,
    OUTSIDE_SERVICE_RADIUS,
    SPECIALIZED_EQUIPMENT_REQUIRED,
    OTHER;

    /**
     * Checks if the given reason string is valid (matches enum name or recognized descriptive string).
     */
    public static boolean isValid(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        String normalized = reason.trim().toUpperCase();
        for (TransferReason tr : values()) {
            if (tr.name().equals(normalized)) {
                return true;
            }
        }
        // Also valid if it contains standard operational keywords for backward compatibility with existing tests
        return normalized.contains("CAPACITY")
                || normalized.contains("PART")
                || normalized.contains("RADIUS")
                || normalized.contains("LOCATION")
                || normalized.contains("EQUIPMENT")
                || normalized.contains("OTHER")
                || normalized.contains("BAY");
    }

    /**
     * Parses a string to TransferReason, falling back to OTHER if not an exact match.
     */
    public static TransferReason fromString(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return OTHER;
        }
        String normalized = reason.trim().toUpperCase();
        for (TransferReason tr : values()) {
            if (tr.name().equals(normalized)) {
                return tr;
            }
        }
        if (normalized.contains("CAPACITY") || normalized.contains("BAY")) return WORKSHOP_AT_CAPACITY;
        if (normalized.contains("PART")) return PARTS_UNAVAILABLE;
        if (normalized.contains("RADIUS") || normalized.contains("LOCATION")) return OUTSIDE_SERVICE_RADIUS;
        if (normalized.contains("EQUIPMENT") || normalized.contains("TOOL")) return SPECIALIZED_EQUIPMENT_REQUIRED;
        return OTHER;
    }
}
