package com.co.eatupapi.dto.inventory.product;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateMessageDTO {

    private UUID id;
    private ProductRequestDTO data;
}