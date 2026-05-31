package com.co.eatupapi.controllers.commercial.client;

import com.co.eatupapi.dto.commercial.client.ClientAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.client.ClientDTO;
import com.co.eatupapi.dto.commercial.client.ClientStatusUpdateDTO;
import com.co.eatupapi.services.commercial.client.ClientService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/commercial/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }


    @PostMapping
    public ResponseEntity<ClientAsyncResponseDTO> createClient(@RequestBody ClientDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(clientService.createClient(request));
    }


    @GetMapping("/{clientId}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable String clientId) {
        ClientDTO client = clientService.getClientById(clientId);
        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getClients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean applyDiscounts) {

        List<ClientDTO> clients = clientService.getClients(name, email, city, documentNumber, active, applyDiscounts);
        return ResponseEntity.ok(clients);
    }


    @PutMapping("/{clientId}")
    public ResponseEntity<ClientAsyncResponseDTO> updateClient(
            @PathVariable String clientId,
            @RequestBody ClientDTO request) {

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(clientService.updateClient(clientId, request));
    }


    @PatchMapping("/{clientId}/status")
    public ResponseEntity<ClientAsyncResponseDTO> updateStatus(
            @PathVariable String clientId,
            @RequestBody ClientStatusUpdateDTO request) {

        Boolean active = request == null ? null : request.getActive();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(clientService.updateStatus(clientId, active));
    }
}
