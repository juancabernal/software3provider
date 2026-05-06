package com.co.eatupapi.dto.inventory.recipe;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecipeResponse {

    private UUID id;
    private String name;
    private UUID categoryId;
    private UUID locationId;

    private List<RecipeProductRequest> products;
    private List<RecipeSubRecipeRequest> subRecipes;

    private BigDecimal baseCost;
    private Integer profitMargin;
    private BigDecimal sellingPrice;

    private Boolean visibleInMenu;
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}