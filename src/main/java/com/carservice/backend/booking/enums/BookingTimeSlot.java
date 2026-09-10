package com.carservice.backend.booking.enums;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum BookingTimeSlot {
    SLOT_09_10("09:00-10:00", LocalTime.of(9, 0), LocalTime.of(10, 0), "09:00 AM – 10:00 AM"),
    SLOT_10_11("10:00-11:00", LocalTime.of(10, 0), LocalTime.of(11, 0), "10:00 AM – 11:00 AM"),
    SLOT_11_12("11:00-12:00", LocalTime.of(11, 0), LocalTime.of(12, 0), "11:00 AM – 12:00 PM"),
    SLOT_12_13("12:00-13:00", LocalTime.of(12, 0), LocalTime.of(13, 0), "12:00 PM – 01:00 PM"),
    SLOT_14_15("14:00-15:00", LocalTime.of(14, 0), LocalTime.of(15, 0), "02:00 PM – 03:00 PM"),
    SLOT_15_16("15:00-16:00", LocalTime.of(15, 0), LocalTime.of(16, 0), "03:00 PM – 04:00 PM"),
    SLOT_16_17("16:00-17:00", LocalTime.of(16, 0), LocalTime.of(17, 0), "04:00 PM – 05:00 PM"),
    SLOT_17_18("17:00-18:00", LocalTime.of(17, 0), LocalTime.of(18, 0), "05:00 PM – 06:00 PM");

    private final String slot;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String displayLabel;

    BookingTimeSlot(String slot, LocalTime startTime, LocalTime endTime, String displayLabel) {
        this.slot = slot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.displayLabel = displayLabel;
    }

    public String getSlot() {
        return slot;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public static boolean isValid(String slotString) {
        if (slotString == null || slotString.trim().isEmpty()) {
            return false;
        }
        String trimmed = slotString.trim();
        return Arrays.stream(values()).anyMatch(s -> s.slot.equalsIgnoreCase(trimmed));
    }

    public static Optional<BookingTimeSlot> fromSlot(String slotString) {
        if (slotString == null) {
            return Optional.empty();
        }
        String trimmed = slotString.trim();
        return Arrays.stream(values())
                .filter(s -> s.slot.equalsIgnoreCase(trimmed))
                .findFirst();
    }

    public static BookingTimeSlot fromBookingTime(LocalTime time) {
        if (time == null) {
            return SLOT_10_11;
        }
        return Arrays.stream(values())
                .filter(s -> !time.isBefore(s.startTime) && time.isBefore(s.endTime))
                .findFirst()
                .orElse(SLOT_10_11);
    }

    public static List<String> getAllSlots() {
        return Arrays.stream(values()).map(BookingTimeSlot::getSlot).toList();
    }
}
