package com.exelynt.booking.repository;

import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    boolean existsByResourceId(Long resourceId);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.resource.id = :resourceId
              and r.status <> :excludedStatus
              and (:excludeReservationId is null or r.id <> :excludeReservationId)
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("resourceId") Long resourceId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("excludeReservationId") Long excludeReservationId,
                              @Param("excludedStatus") ReservationStatus excludedStatus);
}
