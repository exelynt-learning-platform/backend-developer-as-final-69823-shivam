package com.exelynt.booking.repository;

import com.exelynt.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Extends {@link JpaSpecificationExecutor} so the optional status/minPrice/maxPrice filters
 * can be composed into a single query rather than one finder method per combination.
 */
public interface ReservationRepository
        extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
}
