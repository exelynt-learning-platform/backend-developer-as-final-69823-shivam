package com.exelynt.booking.enums;

/**
 * Lifecycle of a reservation.
 *
 * <p>A newly created reservation starts as {@link #PENDING}. An administrator may move it to
 * {@link #CONFIRMED}. Either the owner or an administrator may move it to {@link #CANCELLED},
 * which frees the time slot for other bookings.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
