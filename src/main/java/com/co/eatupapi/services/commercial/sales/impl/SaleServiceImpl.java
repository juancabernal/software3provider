package com.co.eatupapi.services.commercial.sales.impl;

import com.co.eatupapi.domain.commercial.sales.SaleDomain;
import com.co.eatupapi.domain.commercial.sales.SaleStatus;
import com.co.eatupapi.dto.commercial.sales.SaleAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.sales.SaleDeleteRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SaleDetailDTO;
import com.co.eatupapi.dto.commercial.sales.SalePatchDTO;
import com.co.eatupapi.dto.commercial.sales.SalePatchRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import com.co.eatupapi.dto.commercial.sales.SaleUpdateRequestedMessage;
import com.co.eatupapi.messaging.commercial.sales.SaleEventPublisher;
import com.co.eatupapi.repositories.commercial.sales.SaleRepository;
import com.co.eatupapi.services.commercial.sales.SaleService;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleBusinessException;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleNotFoundException;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleValidationException;
import com.co.eatupapi.utils.commercial.sales.mapper.SaleMapper;
import com.co.eatupapi.utils.commercial.sales.validation.ValidationUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleServiceImpl implements SaleService {

    private static final String VENTA_NO_ENCONTRADA = "No existe una venta con el id: ";

    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;
    private final SaleEventPublisher saleEventPublisher;

    public SaleServiceImpl(SaleRepository saleRepository,
                           SaleMapper saleMapper,
                           SaleEventPublisher saleEventPublisher) {
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
        this.saleEventPublisher = saleEventPublisher;
    }

    @Override
    public SaleAsyncResponseDTO createSale(SaleRequestDTO request) {
        validateRequiredSalePayload(request);
        validateSaleLineItems(request.getDetails());
        saleEventPublisher.publishCreateRequested(request);
        return new SaleAsyncResponseDTO("La venta fue recibida y será procesada.", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDTO getSaleById(UUID id) {
        return saleMapper.toDto(findSaleOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponseDTO> getAllSales() {
        return saleRepository.findAll().stream().map(saleMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleAsyncResponseDTO updateSale(UUID id, SaleRequestDTO request) {
        SaleDomain existingSale = findSaleOrThrow(id);
        validateRequiredSalePayload(request);
        validateSaleLineItems(request.getDetails());

        SaleUpdateRequestedMessage message = new SaleUpdateRequestedMessage(saleMapper.toDto(existingSale), request);
        saleEventPublisher.publishUpdateRequested(message);

        return new SaleAsyncResponseDTO("La solicitud de actualización fue recibida y será procesada.", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleAsyncResponseDTO patchSale(UUID id, SalePatchDTO request) {
        findSaleOrThrow(id);
        ValidationUtils.requireObject(request, "El payload de actualización parcial es obligatorio.");
        ValidationUtils.requireObject(request.status(), "El estado es obligatorio.");

        if (request.sellerId() != null || request.locationId() != null || request.tableId() != null || request.details() != null) {
            throw new SaleValidationException("El método PATCH solo permite actualizar el estado de la venta.");
        }

        saleEventPublisher.publishPatchRequested(new SalePatchRequestedMessage(id, request));

        return new SaleAsyncResponseDTO("La solicitud de cambio de estado fue recibida y será procesada.", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleAsyncResponseDTO deleteSale(UUID id) {
        SaleDomain existingSale = findSaleOrThrow(id);
        if (existingSale.getStatus() == SaleStatus.COMPLETED) {
            throw new SaleBusinessException("No se puede eliminar una venta completada.");
        }

        saleEventPublisher.publishDeleteRequested(new SaleDeleteRequestedMessage(saleMapper.toDto(existingSale)));

        return new SaleAsyncResponseDTO("La solicitud de eliminación fue recibida y será procesada.", LocalDateTime.now());
    }

    private SaleDomain findSaleOrThrow(UUID id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException(VENTA_NO_ENCONTRADA + id));
    }

    private void validateRequiredSalePayload(SaleRequestDTO request) {
        ValidationUtils.requireObject(request, "El payload de venta es obligatorio.");
        ValidationUtils.requireText(request.getSellerId(), "sellerId");
        ValidationUtils.requireObject(request.getLocationId(), "La locationId es obligatoria.");
        ValidationUtils.requireText(request.getTableId(), "tableId");
        ValidationUtils.requireObject(request.getDetails(), "La lista de detalles es obligatoria.");

        if (request.getDetails().isEmpty()) {
            throw new SaleValidationException("La venta debe tener al menos una línea de detalle.");
        }
    }

    private void validateSaleLineItems(List<SaleDetailDTO> details) {
        for (SaleDetailDTO detail : details) {
            ValidationUtils.requireObject(detail, "Cada línea de detalle es obligatoria.");
            ValidationUtils.requireObject(detail.getRecipeId(), "El recipeId es obligatorio en cada línea.");
            ValidationUtils.requirePositive(detail.getQuantity(), "La cantidad debe ser mayor que cero.");
            ValidationUtils.requirePositive(detail.getUnitPrice(), "El precio unitario debe ser mayor que cero.");
            ValidationUtils.requireText(detail.getRecipeLineComment(), "recipeLineComment");
            ValidationUtils.validateMaxLength(detail.getRecipeLineComment(), 500, "recipeLineComment");
            ValidationUtils.validateMaxLength(detail.getLineDisplayName(), 255, "lineDisplayName");
        }
    }
}
