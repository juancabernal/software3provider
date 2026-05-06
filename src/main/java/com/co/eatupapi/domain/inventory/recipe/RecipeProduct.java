package com.co.eatupapi.domain.inventory.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Embeddable
public class RecipeProduct {

    @NotNull(message = "RECIPE_PRODUCT_ID_NULL")
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @NotNull(message = "RECIPE_PRODUCT_QUANTITY_REQUIRED")
    @Positive(message = "RECIPE_PRODUCT_QUANTITY_INVALID")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "RECIPE_PRODUCT_PRICE_REQUIRED")
    @PositiveOrZero(message = "RECIPE_PRODUCT_PRICE_INVALID")
    @Column(name = "price", nullable = false, precision = 15, scale = 3)
    private BigDecimal price;
}