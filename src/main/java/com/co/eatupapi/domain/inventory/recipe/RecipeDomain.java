package com.co.eatupapi.domain.inventory.recipe;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
@Entity
@Table(name = "recipes")
public class RecipeDomain {

    @Id
    @NotNull(message = "RECIPE_ID_REQUIRED")
    private UUID id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "RECIPE_NAME_REQUIRED")
    @Size(min = 3, max = 150, message = "RECIPE_NAME_INVALID_LENGTH")
    @Pattern(
            regexp = "^[a-zA-Z0-9ÁÉÍÓÚáéíóúñÑ ]+$",
            message = "RECIPE_NAME_INVALID_FORMAT"
    )
    private String name;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_CATEGORY_REQUIRED")
    private UUID categoryId;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_LOCATION_REQUIRED")
    private UUID locationId;

    @ElementCollection
    @CollectionTable(
            name = "recipe_products",
            joinColumns = @JoinColumn(name = "recipe_id")
    )
    @NotNull(message = "RECIPE_PRODUCTS_REQUIRED")
    @Size(min = 1, message = "RECIPE_PRODUCTS_EMPTY")
    private List<
            @Valid
            @NotNull(message = "RECIPE_PRODUCT_ITEM_NULL")
                    RecipeProduct
            > products;

    @ElementCollection
    @CollectionTable(
            name = "recipe_subrecipes",
            joinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<
            @Valid
            @NotNull(message = "RECIPE_SUBRECIPE_ID_NULL")
                    RecipeSubRecipe
            > subRecipes;

    @Column(nullable = false, precision = 15, scale = 3)
    @NotNull(message = "RECIPE_BASE_COST_REQUIRED")
    @PositiveOrZero(message = "RECIPE_BASE_COST_NEGATIVE")
    private BigDecimal baseCost;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_PROFIT_MARGIN_REQUIRED")
    @PositiveOrZero(message = "RECIPE_PROFIT_MARGIN_NEGATIVE")
    @Max(value = 100, message = "RECIPE_PROFIT_MARGIN_INVALID")
    private Integer profitMargin;

    @Column(nullable = false, precision = 15, scale = 3)
    @NotNull(message = "RECIPE_SELLING_PRICE_REQUIRED")
    @PositiveOrZero(message = "RECIPE_SELLING_PRICE_NEGATIVE")
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_VISIBLE_REQUIRED")
    private Boolean visibleInMenu;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_ACTIVE_REQUIRED")
    private Boolean active;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_CREATED_AT_REQUIRED")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "RECIPE_UPDATED_AT_REQUIRED")
    private LocalDateTime updatedAt;

    public void deactivate() {
        if (Boolean.FALSE.equals(this.active)) {
            return;
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}