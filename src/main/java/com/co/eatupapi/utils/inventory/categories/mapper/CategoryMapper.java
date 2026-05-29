package com.co.eatupapi.utils.inventory.categories.mapper;

import com.co.eatupapi.domain.inventory.categories.CategoryDomain;
import com.co.eatupapi.dto.inventory.categories.CategoryDTO;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryDTO toDto(CategoryDomain categoryDomain) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(categoryDomain.getId() != null ? categoryDomain.getId().toString() : null);
        categoryDTO.setCns(categoryDomain.getCns());
        categoryDTO.setType(categoryDomain.getType());
        categoryDTO.setSubtype(categoryDomain.getSubtype());
        categoryDTO.setName(categoryDomain.getName());
        categoryDTO.setLocationId(categoryDomain.getLocationId() != null ? categoryDomain.getLocationId().toString() : null);
        categoryDTO.setEntryDate(categoryDomain.getEntryDate());
        categoryDTO.setStatus(categoryDomain.getStatus());
        return categoryDTO;
    }

    public CategoryDomain toDomain(CategoryDTO dto) {
        CategoryDomain categoryDomain = new CategoryDomain();

        if (dto.getId() != null && !dto.getId().isBlank()) {
            categoryDomain.setId(UUID.fromString(dto.getId()));
        }

        categoryDomain.setType(dto.getType());
        categoryDomain.setSubtype(dto.getSubtype());
        categoryDomain.setName(dto.getName());
        categoryDomain.setLocationId(parseUuid(dto.getLocationId()));
        categoryDomain.setEntryDate(dto.getEntryDate());
        categoryDomain.setStatus(dto.getStatus());
        categoryDomain.setCns(dto.getCns());
        return categoryDomain;
    }

    /**
     * Solo campos que el cliente puede enviar al crear. Evita parsear {@code id} inválido
     * y no copia auditoría/estado que el servicio asigna después.
     */
    public CategoryDomain toNewEntity(CategoryDTO dto) {
        CategoryDomain entity = new CategoryDomain();
        entity.setType(dto.getType());
        entity.setSubtype(dto.getSubtype());
        entity.setName(dto.getName());
        entity.setLocationId(parseUuid(dto.getLocationId()));
        return entity;
    }

    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
