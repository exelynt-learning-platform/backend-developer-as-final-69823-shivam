package com.exelynt.booking.dto.response;

import com.exelynt.booking.entity.Resource;

import java.math.BigDecimal;

public record ResourceResponse(
        Long id,
        String name,
        String type,
        String description,
        boolean available,
        BigDecimal pricePerUnit
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.isAvailable(),
                resource.getPricePerUnit());
    }
}
