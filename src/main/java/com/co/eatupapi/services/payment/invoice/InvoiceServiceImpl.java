package com.co.eatupapi.services.payment.invoice;

import com.co.eatupapi.domain.commercial.sales.SaleStatus;
import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.invoice.InvoiceDetail;
import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import com.co.eatupapi.dto.inventory.location.LocationResponseDTO;
import com.co.eatupapi.dto.payment.invoice.InvoiceRequest;
import com.co.eatupapi.dto.payment.invoice.InvoiceResponse;
import com.co.eatupapi.dto.payment.invoice.InvoiceStatusUpdateRequest;
import com.co.eatupapi.messaging.payment.invoice.InvoiceCancelMessage;
import com.co.eatupapi.messaging.payment.invoice.InvoiceCreateMessage;
import com.co.eatupapi.messaging.payment.invoice.InvoiceItemMessage;
import com.co.eatupapi.messaging.payment.invoice.InvoiceMarkPaidMessage;
import com.co.eatupapi.messaging.payment.invoice.InvoiceMessagePublisher;
import com.co.eatupapi.repositories.payment.invoice.InvoiceRepository;
import com.co.eatupapi.services.commercial.customerDiscount.CustomerDiscountService;
import com.co.eatupapi.services.commercial.discount.DiscountService;
import com.co.eatupapi.services.commercial.sales.SaleService;
import com.co.eatupapi.services.inventory.location.LocationService;
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
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final String INVOICE_NOT_FOUND = "Invoice not found";
    private static final String LOCATION_ID_REQUIRED = "Location ID is required";
    private static final String INVOICE_ID_REQUIRED = "Invoice ID is required";
    private static final String SALES_ID_REQUIRED = "Sales ID is required";
    private static final String DISCOUNT_ID_REQUIRED = "Discount ID is required";
    private static final String REQUEST_REQUIRED = "Request body is required";
    private static final String STATUS_REQUIRED = "Status is required";
    private static final String INVOICE_NUMBER_REQUIRED = "Invoice number is required";
    private static final String LOCATION_ID_MISMATCH = "Location ID must match locationId path parameter";
    private static final String INVOICE_NUMBER_ALREADY_EXISTS = "Invoice number already exists for this location";
    private static final String INVOICE_DOES_NOT_BELONG_TO_LOCATION = "Invoice does not belong to this location";
    private static final String CUSTOMER_DISCOUNT_LOCATION_MISMATCH =
            "Customer discount does not belong to the requested location";
    private static final String SALE_LOCATION_MISMATCH = "Sale does not belong to the requested location";
    private static final String SALE_ALREADY_INVOICED = "Sale already has an active invoice for this location";
    private static final String INACTIVE_LOCATION = "Location is inactive and cannot issue invoices";
    private static final String INACTIVE_DISCOUNT = "Discount is inactive and cannot be applied to invoice";
    private static final String NO_DISCOUNT_DESCRIPTION = "Sin descuento";
    private static final String ASYNC_STATUS_UPDATE_NOT_SUPPORTED =
            "Status update is not supported asynchronously yet";
    private static final Set<InvoiceStatus> DUPLICATE_ALLOWED_STATUSES =
            EnumSet.of(InvoiceStatus.CANCELLED, InvoiceStatus.VOIDED);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final SaleService saleService;
    private final CustomerDiscountService customerDiscountService;
    private final DiscountService discountService;
    private final LocationService locationService;
    private final InvoiceCalculator invoiceCalculator;
    private final InvoiceStateValidator invoiceStateValidator;
    private final InvoiceFactory invoiceFactory;
    private final InvoiceMessagePublisher invoiceMessagePublisher;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceMapper invoiceMapper,
                              SaleService saleService,
                              CustomerDiscountService customerDiscountService,
                              DiscountService discountService,
                              LocationService locationService,
                              InvoiceCalculator invoiceCalculator,
                              InvoiceStateValidator invoiceStateValidator,
                              InvoiceFactory invoiceFactory,
                              InvoiceMessagePublisher invoiceMessagePublisher) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.saleService = saleService;
        this.customerDiscountService = customerDiscountService;
        this.discountService = discountService;
        this.locationService = locationService;
        this.invoiceCalculator = invoiceCalculator;
        this.invoiceStateValidator = invoiceStateValidator;
        this.invoiceFactory = invoiceFactory;
        this.invoiceMessagePublisher = invoiceMessagePublisher;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(UUID locationId, InvoiceRequest request) {
        validateCreateRequest(locationId, request);

        String invoiceNumber = normalizeInvoiceNumber(request.getInvoiceNumber());
        validateInvoiceNumberUniqueness(invoiceNumber, locationId);
        validateSaleWithoutActiveInvoice(request.getSalesId(), locationId);

        LocationResponseDTO location = resolveLocation(locationId);
        validateLocationIsActive(location);

        SaleResponseDTO sale = resolveSale(request.getSalesId());
        validateSaleBelongsToLocation(sale, locationId);
        validateSaleIsInvoiceable(sale);

        DiscountContext discountContext = resolveDiscountContext(request.getCustomerDiscountId(), locationId);
        InvoiceTotals totals = invoiceCalculator.calculate(sale.getTotalAmount(), discountContext.discountPercentage());

        Invoice invoice = invoiceFactory.create(new CreateInvoiceCommand(
                invoiceNumber,
                locationId,
                request.getSalesId(),
                sale,
                request.getCustomerDiscountId(),
                discountContext.customerId(),
                discountContext.discountId(),
                discountContext.discountPercentage(),
                discountContext.discountDescription(),
                location,
                totals
        ));

        invoice.setId(UUID.randomUUID());
        invoiceMessagePublisher.publishCreate(toCreateMessage(invoice));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getInvoicesByLocation(UUID locationId, Pageable pageable) {
        validateRequired(locationId, LOCATION_ID_REQUIRED);

        return invoiceRepository.findByLocationId(locationId, pageable)
                .map(invoiceMapper::toResponse);
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

        invoice.setStatus(request.getStatus());

        if (request.getStatus() == InvoiceStatus.CANCELLED) {
            invoiceMessagePublisher.publishCancel(new InvoiceCancelMessage(
                    locationId,
                    invoiceId,
                    "Cancelación solicitada",
                    LocalDateTime.now()
            ));
            return invoiceMapper.toResponse(invoice);
        }

        if (request.getStatus() == InvoiceStatus.PAID) {
            invoiceMessagePublisher.publishMarkPaid(new InvoiceMarkPaidMessage(
                    locationId,
                    invoiceId,
                    null,
                    null,
                    null,
                    LocalDateTime.now()
            ));
            return invoiceMapper.toResponse(invoice);
        }

        throw new InvoiceBusinessException(ASYNC_STATUS_UPDATE_NOT_SUPPORTED);
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

    private void validateInvoiceNumberUniqueness(String invoiceNumber, UUID locationId) {
        if (invoiceRepository.existsByInvoiceNumberAndLocationId(invoiceNumber, locationId)) {
            throw new InvoiceBusinessException(INVOICE_NUMBER_ALREADY_EXISTS);
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

    private void validateCustomerDiscountBelongsToLocation(CustomerDiscountDTO customerDiscount, UUID locationId) {
        if (!locationId.equals(customerDiscount.getLocationId())) {
            throw new InvoiceBusinessException(CUSTOMER_DISCOUNT_LOCATION_MISMATCH);
        }
    }

    private void validateInvoiceBelongsToLocation(Invoice invoice, UUID locationId) {
        if (!locationId.equals(invoice.getLocationId())) {
            throw new InvoiceBusinessException(INVOICE_DOES_NOT_BELONG_TO_LOCATION);
        }
    }

    private void validateSaleBelongsToLocation(SaleResponseDTO sale, UUID locationId) {
        if (sale.getLocationId() == null || !locationId.equals(sale.getLocationId())) {
            throw new InvoiceBusinessException(SALE_LOCATION_MISMATCH);
        }
    }

    private void validateSaleIsInvoiceable(SaleResponseDTO sale) {
        if (sale.getTotalAmount() == null) {
            throw new InvoiceValidationException("Sale total amount is required to create invoice");
        }
        if (sale.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvoiceBusinessException("Sale total amount must be greater than zero to create invoice");
        }
        if (sale.getDetails() != null && sale.getDetails().isEmpty()) {
            throw new InvoiceBusinessException("Sale must have at least one detail to create invoice");
        }
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new InvoiceBusinessException("Cancelled sale cannot be invoiced");
        }
    }

    private void validateLocationIsActive(LocationResponseDTO location) {
        if (!location.isActive()) {
            throw new InvoiceBusinessException(INACTIVE_LOCATION);
        }
    }

    private void validateDiscountIsActive(DiscountDTO discount) {
        if (!Boolean.TRUE.equals(discount.getStatus())) {
            throw new InvoiceBusinessException(INACTIVE_DISCOUNT);
        }
    }

    private Invoice findInvoiceById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(INVOICE_NOT_FOUND));
    }

    private SaleResponseDTO resolveSale(UUID salesId) {
        validateRequired(salesId, SALES_ID_REQUIRED);

        try {
            return saleService.getSaleById(salesId);
        } catch (RuntimeException ex) {
            throw new InvoiceValidationException("Sale not found with id: " + salesId);
        }
    }

    private DiscountContext resolveDiscountContext(UUID customerDiscountId, UUID locationId) {
        if (customerDiscountId == null) {
            return DiscountContext.withoutDiscount();
        }

        CustomerDiscountDTO customerDiscount = resolveCustomerDiscount(customerDiscountId);
        validateCustomerDiscountBelongsToLocation(customerDiscount, locationId);

        DiscountDTO discount = resolveDiscount(customerDiscount.getDiscountId());
        validateDiscountIsActive(discount);

        return DiscountContext.withDiscount(customerDiscount, discount);
    }

    private CustomerDiscountDTO resolveCustomerDiscount(UUID customerDiscountId) {
        Optional<CustomerDiscountDTO> customerDiscount = customerDiscountService.getAllCustomerDiscounts()
                .stream()
                .filter(discount -> customerDiscountId.equals(discount.getId()))
                .findFirst();

        return customerDiscount.orElseThrow(
                () -> new InvoiceValidationException("Customer discount not found with id: " + customerDiscountId)
        );
    }

    private DiscountDTO resolveDiscount(UUID discountId) {
        validateRequired(discountId, DISCOUNT_ID_REQUIRED);

        return discountService.getDiscountById(discountId)
                .orElseThrow(() -> new InvoiceValidationException("Discount not found with id: " + discountId));
    }

    private LocationResponseDTO resolveLocation(UUID locationId) {
        validateRequired(locationId, LOCATION_ID_REQUIRED);

        try {
            return locationService.findById(locationId);
        } catch (RuntimeException ex) {
            throw new InvoiceValidationException("Location not found with id: " + locationId);
        }
    }

    private String normalizeInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new InvoiceValidationException(INVOICE_NUMBER_REQUIRED);
        }
        return invoiceNumber.trim();
    }

    private InvoiceCreateMessage toCreateMessage(Invoice invoice) {
        List<InvoiceItemMessage> details = invoice.getDetails() == null
                ? List.of()
                : invoice.getDetails().stream().map(this::toItemMessage).toList();

        return new InvoiceCreateMessage(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getLocationId(),
                invoice.getSalesId(),
                invoice.getCustomerDiscountId(),
                invoice.getCustomerId(),
                invoice.getDiscountId(),
                invoice.getDiscountPercentage(),
                invoice.getDiscountDescription(),
                invoice.getTableId(),
                invoice.getLocationName(),
                invoice.getSubtotal(),
                invoice.getDiscountAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalPrice(),
                invoice.getStatus(),
                invoice.getInvoiceDate(),
                details,
                LocalDateTime.now()
        );
    }

    private InvoiceItemMessage toItemMessage(InvoiceDetail detail) {
        return new InvoiceItemMessage(
                detail.getRecipeId(),
                detail.getItemName(),
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getSubtotal(),
                detail.getDiscountAmount(),
                detail.getTaxAmount(),
                detail.getTotal(),
                detail.getComment()
        );
    }

    private record DiscountContext(
            UUID customerId,
            UUID discountId,
            BigDecimal discountPercentage,
            String discountDescription
    ) {

        private static DiscountContext withoutDiscount() {
            return new DiscountContext(null, null, BigDecimal.ZERO, NO_DISCOUNT_DESCRIPTION);
        }

        private static DiscountContext withDiscount(CustomerDiscountDTO customerDiscount, DiscountDTO discount) {
            Integer percentage = discount.getPercentage();
            return new DiscountContext(
                    customerDiscount.getCustomerId(),
                    customerDiscount.getDiscountId(),
                    percentage == null ? BigDecimal.ZERO : BigDecimal.valueOf(percentage),
                    discount.getDescription()
            );
        }
    }
}
