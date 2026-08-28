package com.exelynt.booking.service;

import com.exelynt.booking.dto.request.ReservationRequest;
import com.exelynt.booking.dto.response.PageResponse;
import com.exelynt.booking.dto.response.ReservationResponse;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.enums.ReservationStatus;
import com.exelynt.booking.enums.Role;
import com.exelynt.booking.exception.BadRequestException;
import com.exelynt.booking.exception.ConflictException;
import com.exelynt.booking.exception.NotFoundException;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ReservationSpecifications;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private static final List<String> SORTABLE_FIELDS =
            List.of("createdAt", "endTime", "id", "price", "startTime", "status");

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReservationResponse create(ReservationRequest request, String username) {
        User user = requireUser(username);
        Resource resource = requireResource(request.resourceId());

        if (!resource.isAvailable()) {
            throw new ConflictException("Resource is not available for booking: " + resource.getName());
        }
        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), null);

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.PENDING)
                .price(request.price())
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> findAll(ReservationStatus status,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice,
                                                     Pageable pageable,
                                                     String username) {
        User user = requireUser(username);
        assertSortable(pageable);

        Specification<Reservation> spec = ReservationSpecifications.fetchAssociations();
        if (user.getRole() != Role.ADMIN) {
            spec = spec.and(ReservationSpecifications.ownedBy(user.getId()));
        }
        if (status != null) {
            spec = spec.and(ReservationSpecifications.hasStatus(status));
        }
        if (minPrice != null) {
            spec = spec.and(ReservationSpecifications.priceAtLeast(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ReservationSpecifications.priceAtMost(maxPrice));
        }

        return PageResponse.from(
                reservationRepository.findAll(spec, pageable).map(ReservationResponse::from));
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id, String username) {
        User user = requireUser(username);
        Reservation reservation = requireReservation(id);
        assertCanAccess(reservation, user);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request, String username) {
        User user = requireUser(username);
        Reservation reservation = requireReservation(id);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAdmin) {
            assertCanAccess(reservation, user);
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new ConflictException(
                        "Only PENDING reservations can be modified. Current status: " + reservation.getStatus());
            }
        }

        Resource resource = requireResource(request.resourceId());
        if (!resource.isAvailable() && !resource.getId().equals(reservation.getResource().getId())) {
            throw new ConflictException("Resource is not available for booking: " + resource.getName());
        }
        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), reservation.getId());

        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPrice(request.price());
        if (isAdmin && request.status() != null) {
            reservation.setStatus(request.status());
        }

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id, String username) {
        User user = requireUser(username);
        Reservation reservation = requireReservation(id);
        assertCanAccess(reservation, user);

        reservationRepository.delete(reservation);
    }

    private void assertNoOverlap(Long resourceId, LocalDateTime startTime,
                                 LocalDateTime endTime, Long excludeReservationId) {
        if (reservationRepository.existsOverlapping(
                resourceId, startTime, endTime, excludeReservationId, ReservationStatus.CANCELLED)) {
            log.warn("Rejected overlapping reservation for resource {} between {} and {}",
                    resourceId, startTime, endTime);
            throw new ConflictException("The resource is already reserved for an overlapping time period");
        }
    }

    private void assertCanAccess(Reservation reservation, User user) {
        if (user.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(user.getId())) {
            log.warn("User '{}' attempted to access reservation {} belonging to user {}",
                    user.getUsername(), reservation.getId(), reservation.getUser().getId());
            throw new AccessDeniedException("You may only access your own reservations");
        }
    }

    private void assertSortable(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Sortable fields: " + String.join(", ", SORTABLE_FIELDS));
            }
        });
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found: " + username));
    }

    private Resource requireResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + resourceId));
    }

    private Reservation requireReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found with id: " + id));
    }
}
