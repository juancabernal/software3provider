package com.co.eatupapi.services.inventory.location;

import com.co.eatupapi.dto.inventory.location.LocationPatchDTO;
import com.co.eatupapi.dto.inventory.location.LocationRequestDTO;
import com.co.eatupapi.dto.inventory.location.LocationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface LocationService {
    List<LocationResponseDTO> findAll();

    List<LocationResponseDTO> findAllActive();

    LocationResponseDTO findById(UUID id);

    void create(LocationRequestDTO request);

    void update(UUID id, LocationRequestDTO request);

    void patchPartial(UUID id, LocationPatchDTO patch);
}
