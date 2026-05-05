package com.co.eatupapi.services.commercial.customerDiscount;

import com.co.eatupapi.domain.commercial.customerDiscount.CustomerDiscountDomain;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.repositories.commercial.customerDiscount.CustomerDiscountRepository;
import com.co.eatupapi.utils.commercial.customerDiscount.mapper.CustomerDiscountMapper;
import com.co.eatupapi.repositories.commercial.discount.DiscountRepository;
import com.co.eatupapi.domain.commercial.discount.DiscountDomain;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerDiscountServiceImpl implements CustomerDiscountService {

    private final CustomerDiscountRepository customerDiscountRepository;
    private final CustomerDiscountMapper customerDiscountMapper;
    private final DiscountRepository discountRepository;

    public CustomerDiscountServiceImpl(
            CustomerDiscountRepository customerDiscountRepository,
            CustomerDiscountMapper customerDiscountMapper,
            DiscountRepository discountRepository
    ) {
        this.customerDiscountRepository = customerDiscountRepository;
        this.customerDiscountMapper = customerDiscountMapper;
        this.discountRepository = discountRepository;
    }

    @Override
    public List<CustomerDiscountDTO> getAllCustomerDiscounts() {
        return customerDiscountRepository.findAll().stream().map(customerDiscountMapper::toDto).toList();
    }
    @Override
    public CustomerDiscountDTO getCustomerDiscountById(UUID customerDiscountId) {
        CustomerDiscountDomain domain = customerDiscountRepository.findById(customerDiscountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un CustomerDiscount con id: " + customerDiscountId));
        return customerDiscountMapper.toDto(domain);
    }

    @Override
    public List<CustomerDiscountDTO> getDiscountsByCustomerId(UUID customerId) {
        return customerDiscountRepository.findByCustomerId(customerId).stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    public List<CustomerDiscountDTO> getCustomersByDiscountId(UUID discountId) {
        return customerDiscountRepository.findByDiscountId(discountId)
                .stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    public List<CustomerDiscountDTO> getDiscountsByCustomerAndLocation(UUID customerId, UUID locationId) {
        return customerDiscountRepository.findByCustomerIdAndLocationId(customerId, locationId)
                .stream().map(customerDiscountMapper::toDto).toList();
    }

    @Override
    public CustomerDiscountDTO getApplicableCustomerDiscount(
            UUID customerDiscountId,
            UUID customerId,
            UUID locationId) {

        // 1. Busca el CustomerDiscount — lanza si no existe
        CustomerDiscountDomain domain = customerDiscountRepository.findById(customerDiscountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un CustomerDiscount con id: " + customerDiscountId));

        // 2. Valida que pertenezca al cliente
        if (!domain.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "El descuento no pertenece al cliente indicado");
        }

        // 3. Valida que pertenezca a la sede
        if (!domain.getLocationId().equals(locationId)) {
            throw new IllegalArgumentException(
                    "El descuento no pertenece a la sede indicada");
        }

        // 4. Valida que el Discount relacionado esté activo
        // (mismo proyecto → usamos DiscountRepository directamente)
        DiscountDomain discount = discountRepository.findById(domain.getDiscountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El descuento asociado no existe"));

        if (!Boolean.TRUE.equals(discount.getStatus())) {
            throw new IllegalArgumentException(
                    "El descuento asociado no esta activo");
        }
        // 5. Valida vigencia si tiene fechas definidas
        LocalDate hoy = LocalDate.now();

        if (domain.getStartDate() != null && hoy.isBefore(domain.getStartDate())) {
            throw new IllegalArgumentException(
                    "El descuento aun no esta vigente");
        }

        if (domain.getEndDate() != null && hoy.isAfter(domain.getEndDate())) {
            throw new IllegalArgumentException(
                    "El descuento ya vencio");
        }

        return customerDiscountMapper.toDto(domain);
    }

    @Override
    public CustomerDiscountDTO createCustomerDiscount(CustomerDiscountDTO customerDiscount) {
        CustomerDiscountDTO validated = validate(customerDiscount, null);
        CustomerDiscountDomain domain = customerDiscountMapper.toDomain(validated);
        CustomerDiscountDomain saved = customerDiscountRepository.save(domain);
        return customerDiscountMapper.toDto(saved);
    }

    @Override
    public Optional<CustomerDiscountDTO> updateCustomerDiscount(UUID id, CustomerDiscountDTO customerDiscount) {
        CustomerDiscountDTO validated = validate(customerDiscount, id);
        return customerDiscountRepository.findById(id)
                .map(existing -> {
                    customerDiscountMapper.updateDomain(existing, validated);
                    existing.setModifiedAt(LocalDateTime.now());
                    return customerDiscountRepository.save(existing);
                })
                .map(customerDiscountMapper::toDto);
    }

    @Override
    public boolean deleteCustomerDiscount(UUID id) {
        if (!customerDiscountRepository.existsById(id)) {
            return false;
        }
        customerDiscountRepository.deleteById(id);
        return true;
    }

    private CustomerDiscountDTO validate(CustomerDiscountDTO customerDiscount, UUID excludeId) {
        if (customerDiscount.getLocationId() == null)
            throw new IllegalArgumentException("locationId es obligatorio");
        if (customerDiscount.getCustomerId() == null)
            throw new IllegalArgumentException("customerId es obligatorio");
        if (customerDiscount.getDiscountId() == null)
            throw new IllegalArgumentException("discountId es obligatorio");
        if (customerDiscount.getAssignedAt() != null
                && customerDiscount.getAssignedAt().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("assignedAt no puede ser una fecha futura");
        if (customerDiscount.getAssignedAt() == null)
            customerDiscount.setAssignedAt(LocalDate.now());
        if (customerDiscount.getStartDate() != null && customerDiscount.getEndDate() != null
                && customerDiscount.getEndDate().isBefore(customerDiscount.getStartDate()))
            throw new IllegalArgumentException("endDate no puede ser anterior a startDate");

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
            throw new IllegalArgumentException(
                    "Ya existe un descuento asignado a este cliente en esta sede con las mismas fechas");

        return customerDiscount;
    }
}
