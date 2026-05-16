package com.co.eatupapi.utils.commercial.discount.mapper;


import com.co.eatupapi.domain.commercial.discount.DiscountDomain;
import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

    public DiscountDTO toDto(DiscountDomain domain) {
        DiscountDTO dto = new DiscountDTO(
                domain.getId(),
                domain.getCategoryId(),
                domain.getPercentage(),
                domain.getDescription(),
                domain.getStatus()
        );
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setModifiedAt(domain.getModifiedAt());
        return dto;
    }

    public DiscountDomain toDomain(DiscountDTO dto) {
        return new DiscountDomain(
                dto.getId(),
                dto.getCategoryId(),
                dto.getPercentage(),
                dto.getDescription(),
                dto.getStatus()
        );
    }

    public void updateDomain(DiscountDomain domain, DiscountDTO dto) {
        domain.setCategoryId(dto.getCategoryId());
        domain.setPercentage(dto.getPercentage());
        domain.setDescription(dto.getDescription());
        domain.setStatus(dto.getStatus());
    }
}