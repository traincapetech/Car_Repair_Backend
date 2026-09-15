package com.carservice.backend.marketplace.enums;

/**
 * Strict lifecycle states for a service job executed on the workshop floor.
 */
public enum WorkshopJobStatus {
    /**
     * Opportunity has been won and officially assigned to workshop. Awaiting appointment confirmation.
     */
    ASSIGNED,

    /**
     * Workshop has confirmed the appointment schedule with the customer.
     */
    CONFIRMED,

    /**
     * Customer has dropped off vehicle at workshop; physical intake complete.
     */
    VEHICLE_RECEIVED,

    /**
     * Technician is conducting initial inspection and diagnostics.
     */
    INSPECTION,

    /**
     * Service repairs and maintenance operations are underway in the bay.
     */
    WORK_IN_PROGRESS,

    /**
     * All service items complete, quality checks passed, vehicle washed and ready for customer pickup.
     */
    READY_FOR_DELIVERY,

    /**
     * Vehicle handed over to customer, service lifecycle successfully concluded.
     */
    COMPLETED,

    /**
     * Booking/Job was cancelled before physical service intake began.
     */
    CANCELLED,

    /**
     * Workshop transferred the opportunity to the platform for re-matching.
     */
    TRANSFERRED
}
