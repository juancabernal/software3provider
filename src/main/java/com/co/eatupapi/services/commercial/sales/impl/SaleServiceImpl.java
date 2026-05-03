package com.co.eatupapi.services.commercial.sales.impl;

import com.co.eatupapi.domain.commercial.sales.SaleDetailDomain;
import com.co.eatupapi.domain.commercial.sales.SaleDomain;
import com.co.eatupapi.domain.commercial.sales.SaleStatus;
import com.co.eatupapi.dto.commercial.sales.SaleAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.sales.SaleDetailDTO;
import com.co.eatupapi.dto.commercial.sales.SalePatchDTO;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import com.co.eatupapi.messaging.commercial.sales.SaleEventPublisher;
import com.co.eatupapi.repositories.commercial.sales.SaleRepository;
import com.co.eatupapi.services.commercial.sales.RecipePreparationTraceService;
import com.co.eatupapi.services.commercial.sales.SaleService;
import com.co.eatupapi.services.commercial.sales.SaleStockValidatorService;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleBusinessException;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleNotFoundException;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleValidationException;
import com.co.eatupapi.utils.commercial.sales.mapper.SaleMapper;
import com.co.eatupapi.utils.commercial.sales.validation.ValidationUtils;
import java.math.BigDecimal;
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
    private final SaleStockValidatorService saleStockValidatorService;
    private final RecipePreparationTraceService traceService;
    private final SaleEventPublisher saleEventPublisher;

    public SaleServiceImpl(SaleRepository saleRepository,
                           SaleMapper saleMapper,
                           SaleStockValidatorService saleStockValidatorService,
                           RecipePreparationTraceService traceService,
                           SaleEventPublisher saleEventPublisher) {
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
        this.saleStockValidatorService = saleStockValidatorService;
        this.traceService = traceService;
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
        SaleDomain sale = findSaleOrThrow(id);
        return saleMapper.toDto(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponseDTO> getAllSales() {
        return saleRepository.findAll().stream()
                .map(saleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SaleResponseDTO updateSale(UUID id, SaleRequestDTO request) {
        SaleDomain existingSale = findSaleOrThrow(id);
        ensureSaleCanBeModified(existingSale);

        validateRequiredSalePayload(request);
        validateSaleLineItems(request.getDetails());
        saleStockValidatorService.validateStockForSaleDetails(request.getDetails());

        existingSale.setSellerId(request.getSellerId().trim());
        existingSale.setLocationId(request.getLocationId());
        existingSale.setTableId(request.getTableId().trim());

        traceService.deleteTracesBySaleId(existingSale.getId());
        existingSale.getDetails().clear();

        BigDecimal totalAmount = processSaleDetails(existingSale, request.getDetails());
        existingSale.setTotalAmount(totalAmount);

        SaleDomain savedSale = saleRepository.save(existingSale);
        traceService.createInitialTraces(savedSale);

        return saleMapper.toDto(savedSale);
    }

    @Override
    @Transactional
    public SaleResponseDTO patchSale(UUID id, SalePatchDTO request) {
        SaleDomain existingSale = findSaleOrThrow(id);
        ensureSaleCanBeModified(existingSale);

        updateSaleBasicInfo(existingSale, request);
        boolean detailsUpdated = updateSaleDetails(existingSale, request.details());

        validatePersistableSale(existingSale);

        SaleDomain savedSale = saleRepository.save(existingSale);

        if (detailsUpdated) {
            traceService.createInitialTraces(savedSale);
        }

        return saleMapper.toDto(savedSale);
    }

    @Override
    @Transactional
    public void deleteSale(UUID id) {
        SaleDomain existingSale = findSaleOrThrow(id);
        ensureSaleCanBeDeleted(existingSale);

        traceService.deleteTracesBySaleId(existingSale.getId());
        saleRepository.delete(existingSale);
    }

    private SaleDomain findSaleOrThrow(UUID id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException(VENTA_NO_ENCONTRADA + id));
    }

    private void ensureSaleCanBeModified(SaleDomain sale) {
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new SaleBusinessException("No se puede modificar una venta en estado COMPLETED.");
        }
    }

    private void ensureSaleCanBeDeleted(SaleDomain sale) {
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new SaleBusinessException("No se puede eliminar una venta en estado COMPLETED.");
        }
    }

    private void updateSaleBasicInfo(SaleDomain existingSale, SalePatchDTO request) {
        if (request.status() != null) {
            existingSale.setStatus(request.status());
        }

        if (request.sellerId() != null) {
            existingSale.setSellerId(trimToNull(request.sellerId()));
        }

        if (request.locationId() != null) {
            existingSale.setLocationId(request.locationId());
        }

        if (request.tableId() != null) {
            existingSale.setTableId(trimToNull(request.tableId()));
        }
    }

    private boolean updateSaleDetails(SaleDomain existingSale, List<SaleDetailDTO> details) {
        if (details == null) {
            return false;
        }

        if (details.isEmpty()) {
            throw new SaleValidationException("La venta debe tener al menos una línea de detalle.");
        }

        validateSaleLineItems(details);
        saleStockValidatorService.validateStockForSaleDetails(details);

        traceService.deleteTracesBySaleId(existingSale.getId());
        existingSale.getDetails().clear();

        BigDecimal totalAmount = processSaleDetails(existingSale, details);
        existingSale.setTotalAmount(totalAmount);

        return true;
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

    private void validatePersistableSale(SaleDomain sale) {
        ValidationUtils.requireText(sale.getSellerId(), "sellerId");
        ValidationUtils.requireObject(sale.getLocationId(), "La locationId es obligatoria.");
        ValidationUtils.requireText(sale.getTableId(), "tableId");
        ValidationUtils.requireObject(sale.getDetails(), "La lista de detalles es obligatoria.");

        if (sale.getDetails().isEmpty()) {
            throw new SaleValidationException("La venta debe tener al menos una línea de detalle.");
        }
    }

    private BigDecimal processSaleDetails(SaleDomain sale, List<SaleDetailDTO> detailDtos) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleDetailDTO detailDto : detailDtos) {
            SaleDetailDomain detail = new SaleDetailDomain();
            detail.setRecipeId(detailDto.getRecipeId());
            detail.setQuantity(detailDto.getQuantity());
            detail.setUnitPrice(detailDto.getUnitPrice());
            detail.setRecipeLineComment(detailDto.getRecipeLineComment().trim());
            detail.setLineDisplayName(trimToNull(detailDto.getLineDisplayName()));

            BigDecimal subtotal = calculateSubtotal(detailDto.getUnitPrice(), detailDto.getQuantity());
            detail.setSubtotal(subtotal);

            sale.addDetail(detail);
            totalAmount = totalAmount.add(subtotal);
        }

        return totalAmount;
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, BigDecimal quantity) {
        return unitPrice.multiply(quantity);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}