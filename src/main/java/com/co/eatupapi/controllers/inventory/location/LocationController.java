package com.co.eatupapi.controllers.inventory.location;

import com.co.eatupapi.dto.inventory.location.LocationPatchDTO;
import com.co.eatupapi.dto.inventory.location.LocationRequestDTO;
import com.co.eatupapi.dto.inventory.location.LocationResponseDTO;
import com.co.eatupapi.services.inventory.location.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/inventory/api/v1/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<List<LocationResponseDTO>> findAll() {
        return ResponseEntity.ok(locationService.findAll());
    }


    @GetMapping("/active")
    public ResponseEntity<List<LocationResponseDTO>> findAllActive() {
        return ResponseEntity.ok(locationService.findAllActive());
    }
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody LocationRequestDTO request) {
        locationService.create(request);
        return acceptedResponse("Solicitud de creacion de sede enviada a la cola");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> update(@PathVariable UUID id,
                                                      @Valid @RequestBody LocationRequestDTO request) {
        locationService.update(id, request);
        return acceptedResponse("Solicitud de actualizacion de sede enviada a la cola");
    }

    @PatchMapping("/editar/{id}")
    public ResponseEntity<Map<String, String>> patch(@PathVariable UUID id,
                                                     @Valid @RequestBody LocationPatchDTO patch) {
        locationService.patchPartial(id, patch);
        return acceptedResponse("Solicitud de actualizacion parcial de sede enviada a la cola");
    }

    private ResponseEntity<Map<String, String>> acceptedResponse(String message) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", message));
    }
}
