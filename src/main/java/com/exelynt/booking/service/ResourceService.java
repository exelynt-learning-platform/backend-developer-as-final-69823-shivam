package com.exelynt.booking.service;

import com.exelynt.booking.dto.request.ResourceRequest;
import com.exelynt.booking.dto.response.ResourceResponse;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.exception.ConflictException;
import com.exelynt.booking.exception.NotFoundException;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll().stream()
                .map(ResourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(Long id) {
        return ResourceResponse.from(getOrThrow(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .available(request.available() == null || request.available())
                .pricePerUnit(request.pricePerUnit())
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = getOrThrow(id);

        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setPricePerUnit(request.pricePerUnit());
        if (request.available() != null) {
            resource.setAvailable(request.available());
        }

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = getOrThrow(id);

        if (reservationRepository.existsByResourceId(id)) {
            log.warn("Refused to delete resource {} because reservations reference it", id);
            throw new ConflictException(
                    "Resource cannot be deleted because reservations exist for it. Mark it unavailable instead.");
        }

        resourceRepository.delete(resource);
    }

    private Resource getOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + id));
    }
}
