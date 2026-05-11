package com.co.eatupapi.services.commercial.discount;



import com.co.eatupapi.dto.commercial.discount.DiscountDTO;

import com.co.eatupapi.messaging.commercial.discount.DiscountEventPublisher;
import com.co.eatupapi.repositories.commercial.discount.DiscountRepository;
import com.co.eatupapi.utils.commercial.discount.exceptions.BusinessException;
import com.co.eatupapi.utils.commercial.discount.exceptions.ResourceNotFoundException;
import com.co.eatupapi.utils.commercial.discount.exceptions.ValidationException;
import com.co.eatupapi.utils.commercial.discount.mapper.DiscountMapper;
import org.springframework.stereotype.Service;
import com.co.eatupapi.dto.commercial.discount.DiscountAsyncResponseDTO;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;
    private final DiscountEventPublisher discountEventPublisher;

    public DiscountServiceImpl(DiscountRepository discountRepository,
                               DiscountMapper discountMapper,
                               DiscountEventPublisher discountEventPublisher) {
        this.discountRepository = discountRepository;
        this.discountMapper = discountMapper;
        this.discountEventPublisher = discountEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountDTO> getAllDiscounts() {
        return discountRepository.findAll().stream().map(discountMapper::toDto).toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<DiscountDTO> getActiveDiscounts() {
        return discountRepository.findByStatus(Boolean.TRUE).stream().map(discountMapper::toDto).toList();
    }
//cambiar cuando facturas quite el acoplamiento y poner discount con id no encontrados
    @Override
    @Transactional(readOnly = true)
    public Optional<DiscountDTO> getDiscountById(UUID id) {
        return discountRepository.findById(id)
                .map(discountMapper::toDto);
    }

    @Override
    public DiscountAsyncResponseDTO createDiscount(DiscountDTO discount) {
        DiscountDTO validated = validate(discount, null);
        discountEventPublisher.publishDiscountCreated(validated);
        return new DiscountAsyncResponseDTO("El descuento fue recibido y sera procesado.", LocalDateTime.now());
    }

    @Override
    public DiscountAsyncResponseDTO updateDiscount(UUID id, DiscountDTO discount) {
        if (!discountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Descuento no encontrado con id: " + id);
        }
        DiscountDTO validated = validate(discount, id);
        validated.setId(id);
        discountEventPublisher.publishDiscountUpdated(validated);
        return new DiscountAsyncResponseDTO("La actualizacion del descuento fue recibida y sera procesada.", LocalDateTime.now());
    }

    @Override
    public DiscountAsyncResponseDTO updateDiscountStatus(UUID id, Boolean status) {
        if (status == null) {
            throw new ValidationException("status es obligatorio");
        }
        if (!discountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Descuento no encontrado con id: " + id);
        }
        DiscountDTO dto = new DiscountDTO();
        dto.setId(id);
        dto.setStatus(status);
        discountEventPublisher.publishDiscountStatusUpdated(dto);
        return new DiscountAsyncResponseDTO("El cambio de estado del descuento fue recibido y sera procesado.", LocalDateTime.now());
    }

    @Override
    public DiscountAsyncResponseDTO deleteDiscount(UUID id) {
        if (!discountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Descuento no encontrado con id: " + id);
        }
        discountEventPublisher.publishDiscountDeleted(id);
        return new DiscountAsyncResponseDTO("La eliminacion del descuento fue recibida y sera procesada.", LocalDateTime.now());
    }

    private DiscountDTO validate(DiscountDTO discount, UUID excludeId) {
        if (discount.getStatus() == null)
            discount.setStatus(Boolean.TRUE);

        boolean duplicado = excludeId != null
                ? discountRepository.existsByCategoryIdAndDescriptionAndIdNot(
                discount.getCategoryId(), discount.getDescription(), excludeId)
                : discountRepository.existsByCategoryIdAndDescription(
                discount.getCategoryId(), discount.getDescription());

        if (duplicado)
            throw new BusinessException("Ya existe un descuento con esa descripcion en esta categoria");

        return discount;
    }
}
