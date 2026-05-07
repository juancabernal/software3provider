package com.co.eatupapi.services.commercial.discount;



import com.co.eatupapi.dto.commercial.discount.DiscountDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountService {

    List<DiscountDTO> getAllDiscounts();

    List<DiscountDTO> getActiveDiscounts();

    DiscountDTO getDiscountById(UUID id);

    DiscountDTO createDiscount(DiscountDTO discount);

    DiscountDTO updateDiscount(UUID id, DiscountDTO discount);


    DiscountDTO updateDiscountStatus(UUID id, Boolean status);

    void deleteDiscount(UUID id);
}
