package com.exelynt.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ResourceRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Type is required")
        @Size(max = 60, message = "Type must not exceed 60 characters")
        String type,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        Boolean available,

        @NotNull(message = "Price per unit is required")
        @DecimalMin(value = "0.0", message = "Price per unit must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimals")
        BigDecimal pricePerUnit
) {
}
