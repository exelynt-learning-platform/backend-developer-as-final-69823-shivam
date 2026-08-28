package com.exelynt.booking.controller;

import com.exelynt.booking.dto.request.ResourceRequest;
import com.exelynt.booking.dto.response.ResourceResponse;
import com.exelynt.booking.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> findAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        ResourceResponse created = resourceService.create(request);

        return ResponseEntity
                .created(uriBuilder.path("/api/resources/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
