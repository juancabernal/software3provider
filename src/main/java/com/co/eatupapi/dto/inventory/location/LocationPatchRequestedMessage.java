package com.co.eatupapi.dto.inventory.location;

import java.util.UUID;

public class LocationPatchRequestedMessage {

    private final UUID id;
    private final LocationPatchDTO patch;

    public LocationPatchRequestedMessage(UUID id, LocationPatchDTO patch) {
        this.id = id;
        this.patch = patch;
    }

    public UUID getId() {
        return id;
    }

    public LocationPatchDTO getPatch() {
        return patch;
    }
}
