package com.co.eatupapi.dto.inventory.recipe;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecipeProductRequest {

    public static final String PRODUCT_ID_REQUIRED = "RECIPE_PRODUCT_ID_REQUIRED";
    public static final String PRODUCT_QUANTITY_REQUIRED = "RECIPE_PRODUCT_QUANTITY_REQUIRED";
    public static final String PRODUCT_QUANTITY_INVALID = "RECIPE_PRODUCT_QUANTITY_INVALID";
    public static final String PRODUCT_PRICE_REQUIRED = "RECIPE_PRODUCT_PRICE_REQUIRED";
    public static final String PRODUCT_PRICE_INVALID = "RECIPE_PRODUCT_PRICE_INVALID";

    @NotNull(message = PRODUCT_ID_REQUIRED)
    private UUID productId;

    @NotNull(message = PRODUCT_QUANTITY_REQUIRED)
    @Positive(message = PRODUCT_QUANTITY_INVALID)
    private Integer quantity;

    @NotNull(message = PRODUCT_PRICE_REQUIRED)
    @PositiveOrZero(message = PRODUCT_PRICE_INVALID)
    private BigDecimal price;
}