package com.co.eatupapi.services.commercial.customerDiscount;


import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountAsyncResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CustomerDiscountService {

    List<CustomerDiscountDTO> getAllCustomerDiscounts();

    CustomerDiscountDTO getCustomerDiscountById(UUID customerDiscountId);

    List<CustomerDiscountDTO> getDiscountsByCustomerId(UUID customerId);

    List<CustomerDiscountDTO> getDiscountsByCustomerAndLocation(UUID customerId, UUID locationId);

    CustomerDiscountAsyncResponseDTO createCustomerDiscount(CustomerDiscountDTO customerDiscount);

    CustomerDiscountDTO getApplicableCustomerDiscount(
            UUID customerDiscountId,
            UUID customerId,
            UUID locationId
    );

    List<CustomerDiscountDTO> getCustomersByDiscountId(UUID discountId);

    CustomerDiscountAsyncResponseDTO updateCustomerDiscount(UUID id, CustomerDiscountDTO customerDiscount);
    CustomerDiscountAsyncResponseDTO deleteCustomerDiscount(UUID id);
}
