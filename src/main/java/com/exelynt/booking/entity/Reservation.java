package com.exelynt.booking.entity;

import com.exelynt.booking.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A booking of a {@link Resource} by a {@link User} over a time window.
 *
 * <p>The owning user is always derived from the authenticated principal when a reservation is
 * created — never from client input — so a caller cannot book on someone else's behalf.
 */
@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_user", columnList = "user_id"),
                @Index(name = "idx_reservations_resource_time", columnList = "resource_id, start_time, end_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** Owning side of User 1..* Reservation. Lazy to keep it out of unrelated queries. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_user"))
    private User user;

    /** Owning side of Resource 1..* Reservation. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_resource"))
    private Resource resource;

    @Column(name = "start_time", nullable = false)
    @ToString.Include
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    @ToString.Include
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private ReservationStatus status;

    /** Total price for this booking. NUMERIC(10,2). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
