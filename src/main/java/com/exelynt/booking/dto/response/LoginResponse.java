package com.exelynt.booking.dto.response;

import com.exelynt.booking.enums.Role;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        Role role
) {
}
