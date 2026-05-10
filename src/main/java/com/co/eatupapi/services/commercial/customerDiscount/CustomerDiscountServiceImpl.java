package com.co.eatupapi.services.commercial.customerDiscount;

import com.co.eatupapi.domain.commercial.customerDiscount.CustomerDiscountDomain;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountAsyncResponseDTO;
import java.time.LocalDateTime;
import com.co.eatupapi.messaging.commercial.customerDiscount.CustomerDiscountEventPublisher;
import com.co.eatupapi.repositories.commercial.customerDiscount.CustomerDiscountRepository;
import com.co.eatupapi.utils.commercial.customerDiscount.mapper.CustomerDiscountMapper;
import com.co.eatupapi.repositories.commercial.discount.DiscountRepository;
import com.co.eatupapi.domain.commercial.discount.DiscountDomain;
import com.co.eatupapi.utils.commercial.customerDiscount.exceptions.BusinessException;
import com.co.eatupapi.utils.commercial.customerDiscount.exceptions.ResourceNotFoundException;
import com.co.eatupapi.utils.commercial.customerDiscount.exceptions.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerDiscountServiceImpl implements CustomerDiscountService {

    private final CustomerDiscountRepository customerDiscountRepository;
    private final CustomerDiscountMapper customerDiscountMapper;
    private final DiscountRepository discountRepository;
    private final CustomerDiscountEventPublisher customerDiscountEventPublisher;

    public CustomerDiscountServiceImpl(
            CustomerDiscountRepository customerDiscountRepository,
            CustomerDiscountMapper customerDiscountMapper,
            DiscountRepository discountRepository,
            CustomerDiscountEventPublisher customerDiscountEventPublisher
    ) {
        this.customerDiscountRepository = customerDiscountRepository;
        this.customerDiscountMapper = customerDiscountMapper;
        this.discountRepository = discountRepository;
        this.customerDiscountEventPublisher = customerDiscountEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDiscountDTO> getAllCustomerDiscounts() {
        return customerDiscountRepository.findAll().stream()
                .map(customerDiscountMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDiscountDTO getCustomerDiscountById(UUID customerDiscountId) {
        return customerDiscountRepository.findById(customerDiscountId)
                .map(customerDiscountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerDiscount no encontrado con id: " + customerDiscountId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDiscountDTO> getDiscountsByCustomerId(UUID customerId) {
        return customerDiscountRepository.findByCustomerId(customerId)
                .stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDiscountDTO> getCustomersByDiscountId(UUID discountId) {
        return customerDiscountRepository.findByDiscountId(discountId)
                .stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDiscountDTO> getDiscountsByCustomerAndLocation(UUID customerId, UUID locationId) {
        return customerDiscountRepository.findByCustomerIdAndLocationId(customerId, locationId)
                .stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDiscountDTO getApplicableCustomerDiscount(UUID customerDiscountId,
                                                             UUID customerId, UUID locationId) {
        CustomerDiscountDomain domain = customerDiscountRepository.findById(customerDiscountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerDiscount no encontrado con id: " + customerDiscountId));

        if (!domain.getCustomerId().equals(customerId))
            throw new BusinessException("El descuento no pertenece al cliente indicado");
        if (!domain.getLocationId().equals(locationId))
            throw new BusinessException("El descuento no pertenece a la sede indicada");

        DiscountDomain discount = discountRepository.findById(domain.getDiscountId())
                .orElseThrow(() -> new ResourceNotFoundException("El descuento asociado no existe"));
        if (!Boolean.TRUE.equals(discount.getStatus()))
            throw new BusinessException("El descuento asociado no esta activo");

        LocalDate hoy = LocalDate.now();
        if (domain.getStartDate() != null && hoy.isBefore(domain.getStartDate()))
            throw new BusinessException("El descuento aun no esta vigente");
        if (domain.getEndDate() != null && hoy.isAfter(domain.getEndDate()))
            throw new BusinessException("El descuento ya vencio");

        return customerDiscountMapper.toDto(domain);
    }

    @Override
    public CustomerDiscountAsyncResponseDTO createCustomerDiscount(CustomerDiscountDTO customerDiscount) {
        CustomerDiscountDTO validated = validate(customerDiscount, null);
        customerDiscountEventPublisher.publishCustomerDiscountCreated(validated);
        return new CustomerDiscountAsyncResponseDTO("El descuento de cliente fue recibido y sera procesado.", LocalDateTime.now());
    }

    @Override
    public CustomerDiscountAsyncResponseDTO updateCustomerDiscount(UUID id, CustomerDiscountDTO customerDiscount) {
        if (!customerDiscountRepository.existsById(id)) {
            throw new ResourceNotFoundException("CustomerDiscount no encontrado con id: " + id);
        }
        CustomerDiscountDTO validated = validate(customerDiscount, id);
        validated.setId(id);
        customerDiscountEventPublisher.publishCustomerDiscountUpdated(validated);
        return new CustomerDiscountAsyncResponseDTO("La actualizacion del descuento de cliente fue recibida y sera procesada.", LocalDateTime.now());
    }

    @Override
    public CustomerDiscountAsyncResponseDTO deleteCustomerDiscount(UUID id) {
        if (!customerDiscountRepository.existsById(id)) {
            throw new ResourceNotFoundException("CustomerDiscount no encontrado con id: " + id);
        }
        customerDiscountEventPublisher.publishCustomerDiscountDeleted(id);
        return new CustomerDiscountAsyncResponseDTO("La eliminacion del descuento de cliente fue recibida y sera procesada.", LocalDateTime.now());
    }

    private CustomerDiscountDTO validate(CustomerDiscountDTO customerDiscount, UUID excludeId) {
        if (customerDiscount.getAssignedAt() != null
                && customerDiscount.getAssignedAt().isAfter(LocalDate.now()))
            throw new ValidationException("assignedAt no puede ser una fecha futura");
        if (customerDiscount.getAssignedAt() == null)
            customerDiscount.setAssignedAt(LocalDate.now());
        if (customerDiscount.getStartDate() != null && customerDiscount.getEndDate() != null
                && customerDiscount.getEndDate().isBefore(customerDiscount.getStartDate()))
            throw new ValidationException("endDate no puede ser anterior a startDate");

        boolean duplicado = excludeId != null
                ? customerDiscountRepository
                .existsByCustomerIdAndLocationIdAndDiscountIdAndStartDateAndEndDateAndIdNot(
                        customerDiscount.getCustomerId(), customerDiscount.getLocationId(),
                        customerDiscount.getDiscountId(), customerDiscount.getStartDate(),
                        customerDiscount.getEndDate(), excludeId)
                : customerDiscountRepository
                .existsByCustomerIdAndLocationIdAndDiscountIdAndStartDateAndEndDate(
                        customerDiscount.getCustomerId(), customerDiscount.getLocationId(),
                        customerDiscount.getDiscountId(), customerDiscount.getStartDate(),
                        customerDiscount.getEndDate());

        if (duplicado)
            throw new BusinessException("Ya existe un descuento asignado a este cliente en esta sede con las mismas fechas");

        return customerDiscount;
    }
}
