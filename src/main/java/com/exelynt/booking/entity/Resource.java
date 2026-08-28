package com.exelynt.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A bookable item, such as a room, vehicle, or piece of equipment.
 */
@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 120)
    @ToString.Include
    private String name;

    @Column(length = 60)
    @ToString.Include
    private String type;

    @Column(length = 500)
    private String description;

    /** When false the resource is withdrawn from service and new reservations are rejected. */
    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    /** Cost per booked unit. NUMERIC(10,2) — never a floating point type for money. */
    @Column(name = "price_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    /** Inverse side of Resource 1..* Reservation. Lazy, and not cascaded. */
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
