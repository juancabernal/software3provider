package com.co.eatupapi.services.inventory.location.impl;

import com.co.eatupapi.domain.inventory.location.LocationDomain;
import com.co.eatupapi.dto.inventory.location.LocationPatchDTO;
import com.co.eatupapi.dto.inventory.location.LocationPatchRequestedMessage;
import com.co.eatupapi.dto.inventory.location.LocationRequestDTO;
import com.co.eatupapi.dto.inventory.location.LocationResponseDTO;
import com.co.eatupapi.dto.inventory.location.LocationUpdateRequestedMessage;
import com.co.eatupapi.messaging.inventory.location.LocationEventPublisher;
import com.co.eatupapi.repositories.inventory.location.LocationRepository;
import com.co.eatupapi.services.inventory.location.LocationService;
import com.co.eatupapi.utils.inventory.location.exceptions.LocationResourceNotFoundException;
import com.co.eatupapi.utils.inventory.location.exceptions.LocationValidationException;
import com.co.eatupapi.utils.inventory.location.validation.LocationValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationEventPublisher locationEventPublisher;

    public LocationServiceImpl(LocationRepository locationRepository,
                               LocationEventPublisher locationEventPublisher) {
        this.locationRepository = locationRepository;
        this.locationEventPublisher = locationEventPublisher;
    }

    @Override
    public List<LocationResponseDTO> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(LocationResponseDTO::fromDomain)
                .toList();
    }

    @Override
    public List<LocationResponseDTO> findAllActive() {
        return locationRepository.findByActiveTrue()
                .stream()
                .map(LocationResponseDTO::fromDomain)
                .toList();
    }

    @Override
    public LocationResponseDTO findById(UUID id) {
        UUID validatedId = LocationValidator.validateId(id);
        LocationDomain location = locationRepository.findById(validatedId)
                .orElseThrow(() -> new LocationResourceNotFoundException("Sede no encontrada con id: " + validatedId));
        return LocationResponseDTO.fromDomain(location);
    }

    @Override
    public void create(LocationRequestDTO request) {
        locationEventPublisher.publishCreateRequested(request);
    }

    @Override
    public void update(UUID id, LocationRequestDTO request) {
        UUID validatedId = LocationValidator.validateId(id);
        locationEventPublisher.publishUpdateRequested(new LocationUpdateRequestedMessage(validatedId, request));
    }

    @Override
    public void patchPartial(UUID id, LocationPatchDTO patch) {
        UUID validatedId = LocationValidator.validateId(id);
        if (patch == null || isPatchEmpty(patch)) {
            throw new LocationValidationException("Debe enviar al menos un campo para actualizar");
        }

        locationEventPublisher.publishPatchRequested(new LocationPatchRequestedMessage(validatedId, patch));
    }

    private static boolean isPatchEmpty(LocationPatchDTO patch) {
        return patch.getName() == null
                && patch.getCity() == null
                && patch.getAddress() == null
                && patch.getActive() == null
                && patch.getEmail() == null
                && patch.getPhoneNumber() == null
                && patch.getStartTime() == null
                && patch.getEndTime() == null;
    }
}
