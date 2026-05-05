package com.co.eatupapi.services.commercial.discount;



import com.co.eatupapi.domain.commercial.discount.DiscountDomain;
import com.co.eatupapi.dto.commercial.discount.DiscountDTO;

import com.co.eatupapi.repositories.commercial.discount.DiscountRepository;
import com.co.eatupapi.utils.commercial.discount.mapper.DiscountMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;

    public DiscountServiceImpl(DiscountRepository discountRepository, DiscountMapper discountMapper) {
        this.discountRepository = discountRepository;
        this.discountMapper = discountMapper;
    }

    @Override
    public List<DiscountDTO> getAllDiscounts() {
        return discountRepository.findAll().stream().map(discountMapper::toDto).toList();
    }
    @Override
    public List<DiscountDTO> getActiveDiscounts() {
        return discountRepository.findByStatus(Boolean.TRUE).stream().map(discountMapper::toDto).toList();
    }

    @Override
    public Optional<DiscountDTO> getDiscountById(UUID id) {
        return discountRepository.findById(id).map(discountMapper::toDto);
    }

    @Override
    public DiscountDTO createDiscount(DiscountDTO discount) {
        DiscountDTO validated = validate(discount, null);
        DiscountDomain domain = discountMapper.toDomain(validated);
        DiscountDomain saved = discountRepository.save(domain);
        return discountMapper.toDto(saved);
    }

    @Override
    public Optional<DiscountDTO> updateDiscount(UUID id, DiscountDTO discount) {
        DiscountDTO validated = validate(discount, id);
        return discountRepository.findById(id)
                .map(existing -> {
                    discountMapper.updateDomain(existing, validated);
                    existing.setModifiedAt(LocalDateTime.now());
                    return discountRepository.save(existing);
                })
                .map(discountMapper::toDto);
    }

    @Override
    public Optional<DiscountDTO> updateDiscountStatus(UUID id, Boolean status) {
        if (status == null) {
            throw new IllegalArgumentException("status es obligatorio");
        }
        return discountRepository.findById(id)
                .map(existing -> {
                    existing.setStatus(status);
                    existing.setModifiedAt(LocalDateTime.now());
                    return discountRepository.save(existing);
                })
                .map(discountMapper::toDto);
    }

    @Override
    public boolean deleteDiscount(UUID id) {
        if (!discountRepository.existsById(id)) {
            return false;
        }
        discountRepository.deleteById(id);
        return true;
    }

    private DiscountDTO validate(DiscountDTO discount,  UUID excludeId) {

        if (discount.getPercentage() == null)
            throw new IllegalArgumentException("percentage es obligatorio");

        if (discount.getPercentage() < 1 || discount.getPercentage() > 100)
            throw new IllegalArgumentException("percentage debe estar entre 1 y 100");

        if (discount.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId es obligatorio");
        }
        if (discount.getDescription() == null || discount.getDescription().isBlank()) {
            throw new IllegalArgumentException("description es obligatoria");
        }
        if (discount.getDescription().length() < 5 || discount.getDescription().length() > 100) {
            throw new IllegalArgumentException("description debe tener entre 5 y 100 caracteres");
        }
        if (discount.getStatus() == null) {
            discount.setStatus(Boolean.TRUE);
        }
        boolean duplicado = excludeId != null
                ? discountRepository.existsByCategoryIdAndDescriptionAndIdNot(
                discount.getCategoryId(), discount.getDescription(), excludeId)
                : discountRepository.existsByCategoryIdAndDescription(
                discount.getCategoryId(), discount.getDescription());

        if (duplicado) {
            throw new IllegalArgumentException(
                    "Ya existe un descuento con esa descripcion en esta categoria");
        }
        return discount;
    }
}
