package com.co.eatupapi.messaging.inventory.location;

import com.co.eatupapi.dto.inventory.location.LocationPatchRequestedMessage;
import com.co.eatupapi.dto.inventory.location.LocationRequestDTO;
import com.co.eatupapi.dto.inventory.location.LocationUpdateRequestedMessage;

public interface LocationEventPublisher {
    void publishCreateRequested(LocationRequestDTO request);

    void publishUpdateRequested(LocationUpdateRequestedMessage message);

    void publishPatchRequested(LocationPatchRequestedMessage message);
}
