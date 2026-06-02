package com.co.eatupapi.services.commercial.client;

import com.co.eatupapi.dto.commercial.client.ClientDTO;
import java.util.List;

public interface ClientService {
    ClientDTO createClient(ClientDTO request);
    ClientDTO getClientById(String clientId);
    List<ClientDTO> getClients(String name, String email, String city, String documentNumber, Boolean active, Boolean applyDiscounts);
    ClientDTO updateClient(String clientId, ClientDTO request);
    ClientDTO updateStatus(String clientId, Boolean active);
}