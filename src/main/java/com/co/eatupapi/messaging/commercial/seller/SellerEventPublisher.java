package com.co.eatupapi.messaging.commercial.seller;

import com.co.eatupapi.dto.commercial.seller.SellerDTO;
import com.co.eatupapi.dto.commercial.seller.SellerPatchDTO;

public interface SellerEventPublisher {

    void publishSellerCreated(SellerDTO seller);

    void publishSellerUpdated(String sellerId, SellerDTO seller);

    void publishSellerStatusUpdated(String sellerId, String status);

    void publishSellerPatched(String sellerId, SellerPatchDTO sellerPatch);
}
