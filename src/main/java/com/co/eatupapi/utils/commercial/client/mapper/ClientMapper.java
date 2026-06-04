package com.co.eatupapi.utils.commercial.client.mapper;

import com.co.eatupapi.domain.commercial.client.ClientDomain;
import com.co.eatupapi.dto.commercial.client.ClientDTO;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientDTO toDto(ClientDomain client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId() == null ? null : client.getId().toString());
        dto.setFirstName(client.getFirstName());
        dto.setSecondName(normalizeSecondName(client.getFirstName(), client.getSecondName()));
        dto.setFirstLastName(client.getFirstLastName());
        dto.setSecondLastName(client.getSecondLastName());
        dto.setDocumentTypeId(client.getDocumentTypeId());
        dto.setDocumentNumber(client.getDocumentNumber());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setAddress(client.getAddress());
        dto.setCityId(client.getCityId());
        dto.setTaxRegimeId(client.getTaxRegimeId());
        dto.setAssignedSellerId(client.getAssignedSellerId());
        dto.setApplyDiscounts(client.getApplyDiscounts());
        dto.setStatus(client.getStatus());
        return dto;
    }

    public ClientDomain toDomain(ClientDTO dto) {
        ClientDomain client = new ClientDomain();
        client.setFirstName(dto.getFirstName());
        client.setSecondName(normalizeSecondName(dto.getFirstName(), dto.getSecondName()));
        client.setFirstLastName(dto.getFirstLastName());
        client.setSecondLastName(dto.getSecondLastName());
        client.setDocumentTypeId(dto.getDocumentTypeId());
        client.setDocumentNumber(dto.getDocumentNumber());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setAddress(dto.getAddress());
        client.setCityId(dto.getCityId());
        client.setTaxRegimeId(dto.getTaxRegimeId());
        client.setAssignedSellerId(dto.getAssignedSellerId());
        client.setApplyDiscounts(dto.getApplyDiscounts());
        client.setStatus(dto.getStatus());
        return client;
    }

    public void updateDomain(ClientDomain client, ClientDTO dto) {
        if (dto.getFirstName() != null) client.setFirstName(dto.getFirstName());
        if (dto.getSecondName() != null) {
            client.setSecondName(normalizeSecondName(
                    dto.getFirstName() != null ? dto.getFirstName() : client.getFirstName(),
                    dto.getSecondName()));
        }
        if (dto.getFirstLastName() != null) client.setFirstLastName(dto.getFirstLastName());
        if (dto.getSecondLastName() != null) client.setSecondLastName(dto.getSecondLastName());
        if (dto.getDocumentTypeId() != null) client.setDocumentTypeId(dto.getDocumentTypeId());
        if (dto.getPhone() != null) client.setPhone(dto.getPhone());
        if (dto.getAddress() != null) client.setAddress(dto.getAddress());
        if (dto.getCityId() != null) client.setCityId(dto.getCityId());
        if (dto.getTaxRegimeId() != null) client.setTaxRegimeId(dto.getTaxRegimeId());
        if (dto.getAssignedSellerId() != null) client.setAssignedSellerId(dto.getAssignedSellerId());
        if (dto.getApplyDiscounts() != null) client.setApplyDiscounts(dto.getApplyDiscounts());
    }

    private String normalizeSecondName(String firstName, String secondName) {
        if (secondName == null || secondName.isBlank()) {
            return "";
        }

        String trimmedSecondName = secondName.trim();
        if (firstName != null && trimmedSecondName.equalsIgnoreCase(firstName.trim())) {
            return "";
        }

        return trimmedSecondName;
    }
}
