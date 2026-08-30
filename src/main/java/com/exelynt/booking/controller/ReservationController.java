package com.exelynt.booking.controller;

import com.exelynt.booking.dto.request.ReservationRequest;
import com.exelynt.booking.dto.response.PageResponse;
import com.exelynt.booking.dto.response.ReservationResponse;
import com.exelynt.booking.enums.ReservationStatus;
import com.exelynt.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations",
        description = "Bookings for resources. A USER sees and manages only their own; an ADMIN sees and manages all.")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation",
            description = "The owner is taken from the JWT, never from the request body. New reservations always start as PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Resource does not exist"),
            @ApiResponse(responseCode = "409", description = "Resource unavailable or the time slot overlaps an existing booking")
    })
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                      Authentication authentication,
                                                      UriComponentsBuilder uriBuilder) {
        ReservationResponse created = reservationService.create(request, authentication.getName());

        return ResponseEntity
                .created(uriBuilder.path("/api/reservations/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations with filtering, pagination, and sorting",
            description = "A USER sees only their own reservations; an ADMIN sees every user's. "
                    + "Sortable fields: createdAt, endTime, id, price, startTime, status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of reservations"),
            @ApiResponse(responseCode = "400", description = "Unknown status value or unsupported sort field")
    })
    public ResponseEntity<PageResponse<ReservationResponse>> findAll(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price, inclusive") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price, inclusive") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "startTime") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(reservationService.findAll(
                status, minPrice, maxPrice, pageable, authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one reservation by id",
            description = "A USER may only read their own reservation; an ADMIN may read any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "403", description = "The reservation belongs to another user"),
            @ApiResponse(responseCode = "404", description = "No reservation with that id")
    })
    public ResponseEntity<ReservationResponse> findById(@PathVariable Long id,
                                                        Authentication authentication) {
        return ResponseEntity.ok(reservationService.findById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation",
            description = "The owner may edit their own reservation only while it is PENDING. "
                    + "An ADMIN may edit any reservation and is the only role that can change status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "The reservation belongs to another user"),
            @ApiResponse(responseCode = "404", description = "No reservation with that id"),
            @ApiResponse(responseCode = "409", description = "Not PENDING, resource unavailable, or the new slot overlaps")
    })
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(reservationService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation",
            description = "The owner may delete their own reservation; an ADMIN may delete any.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "The reservation belongs to another user"),
            @ApiResponse(responseCode = "404", description = "No reservation with that id")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        reservationService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
