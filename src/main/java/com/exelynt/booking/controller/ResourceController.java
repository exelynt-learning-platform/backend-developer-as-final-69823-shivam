package com.exelynt.booking.controller;

import com.exelynt.booking.dto.request.ResourceRequest;
import com.exelynt.booking.dto.response.ResourceResponse;
import com.exelynt.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Resources", description = "Bookable items. Readable by any authenticated user; only ADMIN may modify.")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "List all resources", description = "Available to USER and ADMIN.")
    public ResponseEntity<List<ResourceResponse>> findAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one resource by id", description = "Available to USER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No resource with that id")
    })
    public ResponseEntity<ResourceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a resource", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; Location header points at the new resource"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        ResourceResponse created = resourceService.create(request);

        return ResponseEntity
                .created(uriBuilder.path("/api/resources/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a resource", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "No resource with that id")
    })
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource",
            description = "ADMIN only. Refused with 409 when reservations reference the resource; mark it unavailable instead.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "No resource with that id"),
            @ApiResponse(responseCode = "409", description = "Reservations exist for this resource")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
