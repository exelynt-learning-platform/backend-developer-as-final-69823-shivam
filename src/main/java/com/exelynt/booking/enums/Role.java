package com.exelynt.booking.enums;

/**
 * Application roles. Stored as a string in the database and mapped to a
 * Spring Security authority of the form {@code ROLE_<name>} at authentication time.
 */
public enum Role {
    ADMIN,
    USER
}
