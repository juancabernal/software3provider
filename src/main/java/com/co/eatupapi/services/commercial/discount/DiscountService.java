package com.co.eatupapi.services.commercial.discount;



import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import com.co.eatupapi.dto.commercial.discount.DiscountAsyncResponseDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountService {

    List<DiscountDTO> getAllDiscounts();

    List<DiscountDTO> getActiveDiscounts();
//cambiar cuando facturas quite el acoplamiento
    Optional<DiscountDTO> getDiscountById(UUID id);

    DiscountAsyncResponseDTO createDiscount(DiscountDTO discount);
    DiscountAsyncResponseDTO updateDiscount(UUID id, DiscountDTO discount);
    DiscountAsyncResponseDTO updateDiscountStatus(UUID id, Boolean status);
    DiscountAsyncResponseDTO deleteDiscount(UUID id);
}
