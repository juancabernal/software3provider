package com.co.eatupapi.services.commercial.client.impl;

import com.co.eatupapi.domain.commercial.client.ClientDomain;
import com.co.eatupapi.domain.commercial.client.ClientStatus;
import com.co.eatupapi.dto.commercial.client.ClientAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.client.ClientDTO;
import com.co.eatupapi.messaging.commercial.client.ClientEventPublisher;
import com.co.eatupapi.repositories.commercial.client.ClientRepository;
import com.co.eatupapi.services.commercial.client.ClientService;
import com.co.eatupapi.utils.commercial.client.exceptions.ClientBusinessException;
import com.co.eatupapi.utils.commercial.client.exceptions.ClientNotFoundException;
import com.co.eatupapi.utils.commercial.client.exceptions.ClientValidationException;
import com.co.eatupapi.utils.commercial.client.mapper.ClientMapper;
import com.co.eatupapi.utils.commercial.client.validation.ClientValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ClientEventPublisher clientEventPublisher;

    public ClientServiceImpl(
            ClientRepository clientRepository,
            ClientMapper clientMapper,
            ClientEventPublisher clientEventPublisher) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.clientEventPublisher = clientEventPublisher;
    }

    @Override
    public ClientAsyncResponseDTO createClient(ClientDTO request) {
        validateCreateRequest(request);
        validateUniqueFields(request);
        validateForeignKeys();

        ClientDomain client = clientMapper.toDomain(request);
        client.setId(UUID.randomUUID());
        client.setStatus(ClientStatus.ACTIVE);
        clientEventPublisher.publishClientCreated(clientMapper.toDto(client));
        return accepted("La creacion del cliente fue recibida y sera procesada.");
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(String clientId) {
        ClientDomain client = findById(clientId);
        return clientMapper.toDto(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getClients(String name, String email, String city, String documentNumber,
                                      Boolean active, Boolean applyDiscounts) {
        List<ClientDomain> clients = clientRepository.findAll();

        return clients.stream()
                .filter(c -> name == null || c.getFirstName().toLowerCase().contains(name.toLowerCase()))
                .filter(c -> email == null || c.getEmail().equalsIgnoreCase(email))
                .filter(c -> documentNumber == null || c.getDocumentNumber().equals(documentNumber))
                .filter(c -> applyDiscounts == null || applyDiscounts.equals(c.getApplyDiscounts()))
                .filter(c -> active == null ||
                        (c.getStatus() != null &&
                                ((active && c.getStatus() == ClientStatus.ACTIVE) ||
                                        (!active && c.getStatus() == ClientStatus.INACTIVE))))
                .map(clientMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientAsyncResponseDTO updateClient(String clientId, ClientDTO request) {
        validateUpdateRequest(request);
        validateForeignKeys();

        ClientDomain client = findById(clientId);
        ClientDTO payload = buildUpdatedPayload(client, request);
        clientEventPublisher.publishClientUpdated(clientId, payload);
        return accepted("La actualizacion del cliente fue recibida y sera procesada.");
    }

    @Override
    @Transactional(readOnly = true)
    public ClientAsyncResponseDTO updateStatus(String clientId, Boolean active) {
        ClientValidationUtils.requireObject(active, "active");

        boolean isActive = active;

        findById(clientId);
        clientEventPublisher.publishClientStatusUpdated(clientId, isActive);
        return accepted("El cambio de estado del cliente fue recibido y sera procesado.");
    }

    private void validateCreateRequest(ClientDTO request) {
        ClientValidationUtils.requireObject(request, "Client request cannot be null");
        ClientValidationUtils.requireText(request.getFirstName(), "firstName");
        ClientValidationUtils.requireText(request.getFirstLastName(), "firstLastName");
        ClientValidationUtils.requireText(request.getSecondLastName(), "secondLastName");
        ClientValidationUtils.requireText(request.getDocumentNumber(), "documentNumber");
        ClientValidationUtils.requireText(request.getEmail(), "email");
        ClientValidationUtils.requireText(request.getPhone(), "phone");
        ClientValidationUtils.requireText(request.getAddress(), "address");
        ClientValidationUtils.requireObject(request.getDocumentTypeId(), "documentTypeId");
        ClientValidationUtils.requireObject(request.getCityId(), "cityId");
        ClientValidationUtils.requireObject(request.getTaxRegimeId(), "taxRegimeId");
        ClientValidationUtils.requireObject(request.getAssignedSellerId(), "assignedSellerId");
        ClientValidationUtils.requireObject(request.getApplyDiscounts(), "applyDiscounts");

        ClientValidationUtils.validateEmail(request.getEmail());
        ClientValidationUtils.validatePhone(request.getPhone());
    }

    private void validateUpdateRequest(ClientDTO request) {
        ClientValidationUtils.requireObject(request, "Client request cannot be null");
        ClientValidationUtils.requireText(request.getFirstName(), "firstName");
        ClientValidationUtils.requireText(request.getFirstLastName(), "firstLastName");
        ClientValidationUtils.requireText(request.getSecondLastName(), "secondLastName");
        ClientValidationUtils.requireText(request.getPhone(), "phone");
        ClientValidationUtils.requireText(request.getAddress(), "address");
        ClientValidationUtils.requireObject(request.getDocumentTypeId(), "documentTypeId");
        ClientValidationUtils.requireObject(request.getCityId(), "cityId");
        ClientValidationUtils.requireObject(request.getTaxRegimeId(), "taxRegimeId");
        ClientValidationUtils.requireObject(request.getAssignedSellerId(), "assignedSellerId");
        ClientValidationUtils.requireObject(request.getApplyDiscounts(), "applyDiscounts");

        ClientValidationUtils.validatePhone(request.getPhone());

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            ClientValidationUtils.validateEmail(request.getEmail());
        }
    }

    private void validateUniqueFields(ClientDTO request) {
        if (clientRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ClientBusinessException("Document number already exists");
        }
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new ClientBusinessException("Email already exists");
        }
    }

    private void validateForeignKeys() {
        // Foreign key validation skipped - other team members use UUID for IDs
        // Validation can be added later using code/name queries or ID standardization
    }

    private ClientDomain findById(String clientId) {
        UUID uuid = parseClientId(clientId);
        return clientRepository.findById(uuid)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
    }

    private UUID parseClientId(String clientId) {
        try {
            return UUID.fromString(clientId);
        } catch (IllegalArgumentException ex) {
            throw new ClientValidationException("Invalid clientId format. Expected UUID value");
        }
    }

    private ClientAsyncResponseDTO accepted(String message) {
        return new ClientAsyncResponseDTO(message, LocalDateTime.now());
    }

    private ClientDTO buildUpdatedPayload(ClientDomain client, ClientDTO request) {
        ClientDTO payload = clientMapper.toDto(client);
        payload.setFirstName(request.getFirstName());
        payload.setSecondName(request.getSecondName());
        payload.setFirstLastName(request.getFirstLastName());
        payload.setSecondLastName(request.getSecondLastName());
        payload.setDocumentTypeId(request.getDocumentTypeId());
        payload.setPhone(request.getPhone());
        payload.setAddress(request.getAddress());
        payload.setCityId(request.getCityId());
        payload.setTaxRegimeId(request.getTaxRegimeId());
        payload.setAssignedSellerId(request.getAssignedSellerId());
        payload.setApplyDiscounts(request.getApplyDiscounts());
        return payload;
    }
}
