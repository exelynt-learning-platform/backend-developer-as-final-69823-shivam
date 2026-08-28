package com.exelynt.booking.controller;

import com.exelynt.booking.dto.request.ReservationRequest;
import com.exelynt.booking.dto.response.PageResponse;
import com.exelynt.booking.dto.response.ReservationResponse;
import com.exelynt.booking.enums.ReservationStatus;
import com.exelynt.booking.service.ReservationService;
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
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                      Authentication authentication,
                                                      UriComponentsBuilder uriBuilder) {
        ReservationResponse created = reservationService.create(request, authentication.getName());

        return ResponseEntity
                .created(uriBuilder.path("/api/reservations/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReservationResponse>> findAll(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "startTime") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(reservationService.findAll(
                status, minPrice, maxPrice, pageable, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findById(@PathVariable Long id,
                                                        Authentication authentication) {
        return ResponseEntity.ok(reservationService.findById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(reservationService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        reservationService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
