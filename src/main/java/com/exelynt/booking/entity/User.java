package com.exelynt.booking.entity;

import com.exelynt.booking.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An account that can authenticate against the API.
 *
 * <p>Mapped to {@code users} rather than {@code user} because {@code user} is a reserved
 * word in PostgreSQL and would require quoting on every generated statement.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 50)
    @ToString.Include
    private String username;

    /** BCrypt hash. Never a plaintext password, and never exposed through a DTO. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private Role role;

    /**
     * Inverse side of User 1..* Reservation. Lazy and deliberately not cascaded: deleting a
     * user must not silently delete their booking history. Reservations are always queried
     * through ReservationRepository rather than traversed from here.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
