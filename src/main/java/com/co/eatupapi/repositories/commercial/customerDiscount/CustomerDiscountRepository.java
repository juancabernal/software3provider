package com.co.eatupapi.repositories.commercial.customerDiscount;

import com.co.eatupapi.domain.commercial.customerDiscount.CustomerDiscountDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomerDiscountRepository extends JpaRepository<CustomerDiscountDomain, UUID> {

    List<CustomerDiscountDomain> findByCustomerId(UUID customerId);

    List<CustomerDiscountDomain> findByDiscountId(UUID discountId);

    List<CustomerDiscountDomain> findByCustomerIdAndLocationId(UUID customerId, UUID locationId);

    boolean existsByCustomerIdAndLocationIdAndDiscountIdAndStartDateAndEndDate(
            UUID customerId, UUID locationId, UUID discountId,
            LocalDate startDate, LocalDate endDate);

    boolean existsByCustomerIdAndLocationIdAndDiscountIdAndStartDateAndEndDateAndIdNot(
            UUID customerId, UUID locationId, UUID discountId,
            LocalDate startDate, LocalDate endDate, UUID id);
}