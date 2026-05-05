package com.co.eatupapi.services.commercial.customerDiscount;


import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerDiscountService {

    List<CustomerDiscountDTO> getAllCustomerDiscounts();

    CustomerDiscountDTO getCustomerDiscountById(UUID customerDiscountId);

    List<CustomerDiscountDTO> getDiscountsByCustomerId(UUID customerId);

    List<CustomerDiscountDTO> getDiscountsByCustomerAndLocation(UUID customerId, UUID locationId);

    CustomerDiscountDTO createCustomerDiscount(CustomerDiscountDTO customerDiscount);

    CustomerDiscountDTO getApplicableCustomerDiscount(
            UUID customerDiscountId,
            UUID customerId,
            UUID locationId
    );

    List<CustomerDiscountDTO> getCustomersByDiscountId(UUID discountId);

    Optional<CustomerDiscountDTO> updateCustomerDiscount(UUID id, CustomerDiscountDTO customerDiscount);

    boolean deleteCustomerDiscount(UUID id);
}
