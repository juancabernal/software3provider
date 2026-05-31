package com.co.eatupapi.messaging.commercial.client;

import com.co.eatupapi.dto.commercial.client.ClientDTO;

public interface ClientEventPublisher {
    void publishClientCreated(ClientDTO client);
    void publishClientUpdated(String clientId, ClientDTO client);
    void publishClientStatusUpdated(String clientId, Boolean active);
}
