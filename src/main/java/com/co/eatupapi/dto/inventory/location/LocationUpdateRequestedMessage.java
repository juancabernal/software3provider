package com.co.eatupapi.dto.inventory.location;

import java.util.UUID;

public class LocationUpdateRequestedMessage {

    private final UUID id;
    private final LocationRequestDTO request;

    public LocationUpdateRequestedMessage(UUID id, LocationRequestDTO request) {
        this.id = id;
        this.request = request;
    }

    public UUID getId() {
        return id;
    }

    public LocationRequestDTO getRequest() {
        return request;
    }
}
