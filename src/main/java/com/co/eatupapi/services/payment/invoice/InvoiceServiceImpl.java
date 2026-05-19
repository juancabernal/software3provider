package com.co.eatupapi.services.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.payment.invoice.InvoiceRequest;
import com.co.eatupapi.dto.payment.invoice.InvoiceResponse;
import com.co.eatupapi.dto.payment.invoice.InvoiceStatusUpdateRequest;
import com.co.eatupapi.dto.payment.invoice.detail.InvoiceDetailRequest;
import com.co.eatupapi.messaging.payment.invoice.InvoiceMessageMapper;
import com.co.eatupapi.messaging.payment.invoice.InvoiceMessagePublisher;
import com.co.eatupapi.repositories.payment.invoice.InvoiceRepository;
import com.co.eatupapi.utils.payment.invoice.calculator.InvoiceCalculator;
import com.co.eatupapi.utils.payment.invoice.calculator.InvoiceCalculator.InvoiceTotals;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceBusinessException;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceNotFoundException;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceValidationException;
import com.co.eatupapi.utils.payment.invoice.factory.InvoiceFactory;
import com.co.eatupapi.utils.payment.invoice.factory.InvoiceFactory.CreateInvoiceCommand;
import com.co.eatupapi.utils.payment.invoice.mapper.InvoiceMapper;
import com.co.eatupapi.utils.payment.invoice.validation.InvoiceStateValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final String INVOICE_NOT_FOUND = "Invoice not found";
    private static final String LOCATION_ID_REQUIRED = "Location ID is required";
    private static final String INVOICE_ID_REQUIRED = "Invoice ID is required";
    private static final String SALES_ID_REQUIRED = "Sales ID is required";
    private static final String REQUEST_REQUIRED = "Request body is required";
    private static final String STATUS_REQUIRED = "Status is required";
    private static final String LOCATION_ID_MISMATCH = "Location ID must match locationId path parameter";
    private static final String INVOICE_DOES_NOT_BELONG_TO_LOCATION = "Invoice does not belong to this location";
    private static final String SALE_ALREADY_INVOICED = "Sale already has an active invoice for this location";
    private static final String LOCATION_NAME_REQUIRED = "Location name is required";
    private static final String DETAILS_REQUIRED = "Invoice must contain at least one detail";
    private static final String DETAIL_ITEM_NAME_REQUIRED = "Detail item name is required";
    private static final String DETAIL_QUANTITY_INVALID = "Detail quantity must be greater than zero";
    private static final String DETAIL_UNIT_PRICE_INVALID = "Detail unit price must be greater than zero";
    private static final String DETAIL_SUBTOTAL_INVALID = "Detail subtotal must be greater than zero";
    private static final String DETAIL_SUBTOTAL_MISMATCH =
            "Detail subtotal must match quantity multiplied by unit price";
    private static final String SUBTOTAL_MUST_MATCH_DETAIL_SUM =
            "Invoice subtotal must match the sum of detail subtotals";
    private static final String TOTAL_MUST_MATCH_CALCULATED =
            "Invoice total amount must match subtotal minus discount";
    private static final String CANCEL_REASON = "Cancelacion solicitada";
    private static final String NO_DISCOUNT_DESCRIPTION = "Sin descuento";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final Set<InvoiceStatus> DUPLICATE_ALLOWED_STATUSES =
            EnumSet.of(InvoiceStatus.CANCELLED, InvoiceStatus.VOIDED);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceCalculator invoiceCalculator;
    private final InvoiceStateValidator invoiceStateValidator;
    private final InvoiceFactory invoiceFactory;
    private final InvoiceMessagePublisher invoiceMessagePublisher;
    private final InvoiceMessageMapper invoiceMessageMapper;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceMapper invoiceMapper,
                              InvoiceCalculator invoiceCalculator,
                              InvoiceStateValidator invoiceStateValidator,
                              InvoiceFactory invoiceFactory,
                              InvoiceMessagePublisher invoiceMessagePublisher,
                              InvoiceMessageMapper invoiceMessageMapper,
                              InvoiceNumberGenerator invoiceNumberGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.invoiceCalculator = invoiceCalculator;
        this.invoiceStateValidator = invoiceStateValidator;
        this.invoiceFactory = invoiceFactory;
        this.invoiceMessagePublisher = invoiceMessagePublisher;
        this.invoiceMessageMapper = invoiceMessageMapper;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(UUID locationId, InvoiceRequest request) {
        validateCreateRequest(locationId, request);
        validateSaleWithoutActiveInvoice(request.getSalesId(), locationId);

        BigDecimal discountPercentage = normalizeDiscountPercentage(request.getDiscountPercentage());
        validateDiscountRange(discountPercentage);
        validateDetailSnapshot(request.getDetails(), request.getSubtotal());

        InvoiceTotals totals = invoiceCalculator.calculate(request.getSubtotal(), discountPercentage);
        validateReportedTotalMatchesCalculatedTotal(request.getTotalAmount(), totals.total());

        Invoice invoice = invoiceFactory.create(new CreateInvoiceCommand(
                invoiceNumberGenerator.nextInvoiceNumber(),
                locationId,
                request.getSalesId(),
                request.getTableId(),
                request.getTableSessionId(),
                request.getCustomerId(),
                request.getDiscountId(),
                discountPercentage,
                normalizeDiscountDescription(request.getDiscountDescription()),
                request.getLocationName(),
                request.getDetails(),
                totals
        ));

        invoice.setId(UUID.randomUUID());
        invoiceMessagePublisher.publishCreate(invoiceMessageMapper.toCreateMessage(invoice));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getInvoicesByLocation(UUID locationId, Pageable pageable) {
        validateRequired(locationId, LOCATION_ID_REQUIRED);
        return invoiceRepository.findByLocationId(locationId, pageable).map(invoiceMapper::toResponse);
    }

    @Override
    public InvoiceResponse getInvoiceById(UUID locationId, UUID invoiceId) {
        validateRequired(locationId, LOCATION_ID_REQUIRED);
        validateRequired(invoiceId, INVOICE_ID_REQUIRED);

        Invoice invoice = findInvoiceById(invoiceId);
        validateInvoiceBelongsToLocation(invoice, locationId);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse updateStatus(UUID locationId, UUID invoiceId, InvoiceStatusUpdateRequest request) {
        validateRequired(locationId, LOCATION_ID_REQUIRED);
        validateRequired(invoiceId, INVOICE_ID_REQUIRED);
        validateStatusUpdateRequest(request);

        Invoice invoice = findInvoiceById(invoiceId);
        validateInvoiceBelongsToLocation(invoice, locationId);
        invoiceStateValidator.validateTransition(invoice.getStatus(), request.getStatus());

        InvoiceStatus previousStatus = invoice.getStatus();
        invoice.setStatus(request.getStatus());

        if (request.getStatus() == InvoiceStatus.CANCELLED || request.getStatus() == InvoiceStatus.VOIDED) {
            invoiceMessagePublisher.publishCancel(
                    invoiceMessageMapper.toCancelMessage(invoice, previousStatus, CANCEL_REASON)
            );
            return invoiceMapper.toResponse(invoice);
        }

        if (request.getStatus() == InvoiceStatus.PAID) {
            invoiceMessagePublisher.publishMarkPaid(invoiceMessageMapper.toMarkPaidMessage(invoice, previousStatus));
            return invoiceMapper.toResponse(invoice);
        }

        invoiceMessagePublisher.publishStatusUpdate(
                invoiceMessageMapper.toStatusUpdateMessage(invoice, previousStatus)
        );
        return invoiceMapper.toResponse(invoice);
    }

    private void validateCreateRequest(UUID locationId, InvoiceRequest request) {
        if (request == null) {
            throw new InvoiceValidationException(REQUEST_REQUIRED);
        }

        validateRequired(locationId, LOCATION_ID_REQUIRED);
        validateRequired(request.getLocationId(), LOCATION_ID_REQUIRED);
        validateRequired(request.getSalesId(), SALES_ID_REQUIRED);

        if (!locationId.equals(request.getLocationId())) {
            throw new InvoiceValidationException(LOCATION_ID_MISMATCH);
        }

        if (request.getLocationName() == null || request.getLocationName().isBlank()) {
            throw new InvoiceValidationException(LOCATION_NAME_REQUIRED);
        }

        validatePositive(request.getSubtotal(), "Subtotal must be greater than zero");
        validatePositive(request.getTotalAmount(), "Total amount must be greater than zero");
    }

    private void validateStatusUpdateRequest(InvoiceStatusUpdateRequest request) {
        if (request == null) {
            throw new InvoiceValidationException(REQUEST_REQUIRED);
        }
        if (request.getStatus() == null) {
            throw new InvoiceValidationException(STATUS_REQUIRED);
        }
    }

    private void validateRequired(Object value, String message) {
        if (value == null) {
            throw new InvoiceValidationException(message);
        }
    }

    private void validateSaleWithoutActiveInvoice(UUID salesId, UUID locationId) {
        if (invoiceRepository.existsBySalesIdAndLocationIdAndStatusNotIn(
                salesId,
                locationId,
                DUPLICATE_ALLOWED_STATUSES
        )) {
            throw new InvoiceBusinessException(SALE_ALREADY_INVOICED);
        }
    }

    private void validateInvoiceBelongsToLocation(Invoice invoice, UUID locationId) {
        if (!locationId.equals(invoice.getLocationId())) {
            throw new InvoiceBusinessException(INVOICE_DOES_NOT_BELONG_TO_LOCATION);
        }
    }

    private void validateDiscountRange(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvoiceBusinessException("Discount percentage cannot be negative");
        }
        if (discountPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new InvoiceBusinessException("Discount percentage cannot be greater than 100");
        }
    }

    private void validateDetailSnapshot(List<InvoiceDetailRequest> details, BigDecimal subtotal) {
        if (details == null || details.isEmpty()) {
            throw new InvoiceValidationException(DETAILS_REQUIRED);
        }

        BigDecimal detailSubtotalSum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (InvoiceDetailRequest detail : details) {
            validateDetail(detail);
            detailSubtotalSum = detailSubtotalSum.add(toMoney(detail.getSubtotal()));
        }

        if (toMoney(subtotal).compareTo(detailSubtotalSum) != 0) {
            throw new InvoiceBusinessException(SUBTOTAL_MUST_MATCH_DETAIL_SUM);
        }
    }

    private void validateDetail(InvoiceDetailRequest detail) {
        if (detail == null) {
            throw new InvoiceValidationException(DETAILS_REQUIRED);
        }
        if (detail.getItemName() == null || detail.getItemName().isBlank()) {
            throw new InvoiceValidationException(DETAIL_ITEM_NAME_REQUIRED);
        }
        validatePositive(detail.getQuantity(), DETAIL_QUANTITY_INVALID);
        validatePositive(detail.getUnitPrice(), DETAIL_UNIT_PRICE_INVALID);
        validatePositive(detail.getSubtotal(), DETAIL_SUBTOTAL_INVALID);

        BigDecimal expectedSubtotal = detail.getQuantity()
                .multiply(detail.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        if (expectedSubtotal.compareTo(toMoney(detail.getSubtotal())) != 0) {
            throw new InvoiceBusinessException(DETAIL_SUBTOTAL_MISMATCH);
        }
    }

    private void validatePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvoiceValidationException(message);
        }
    }

    private void validateReportedTotalMatchesCalculatedTotal(BigDecimal requestedTotal, BigDecimal calculatedTotal) {
        validatePositive(requestedTotal, "Total amount must be greater than zero");
        if (toMoney(requestedTotal).compareTo(toMoney(calculatedTotal)) != 0) {
            throw new InvoiceBusinessException(TOTAL_MUST_MATCH_CALCULATED);
        }
    }

    private BigDecimal normalizeDiscountPercentage(BigDecimal discountPercentage) {
        return discountPercentage == null ? BigDecimal.ZERO : discountPercentage;
    }

    private String normalizeDiscountDescription(String discountDescription) {
        if (discountDescription == null || discountDescription.isBlank()) {
            return NO_DISCOUNT_DESCRIPTION;
        }
        return discountDescription.trim();
    }

    private BigDecimal toMoney(BigDecimal value) {
        if (value == null) {
            throw new InvoiceValidationException("Amount is required");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Invoice findInvoiceById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(INVOICE_NOT_FOUND));
    }
}
