package com.co.eatupapi.dto.inventory.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecipeRequest {

    public static final String NAME_REQUIRED = "RECIPE_NAME_REQUIRED";
    public static final String NAME_LENGTH_INVALID = "RECIPE_NAME_INVALID_LENGTH";
    public static final String NAME_FORMAT_INVALID = "RECIPE_NAME_INVALID_FORMAT";
    public static final String CATEGORY_REQUIRED = "RECIPE_CATEGORY_REQUIRED";
    public static final String PRODUCTS_REQUIRED = "RECIPE_PRODUCTS_REQUIRED";
    public static final String PRODUCTS_EMPTY = "RECIPE_PRODUCTS_EMPTY";
    public static final String PRODUCT_ITEM_NULL = "RECIPE_PRODUCT_ITEM_NULL";
    public static final String SUBRECIPE_ITEM_NULL = "RECIPE_SUBRECIPE_ITEM_NULL";
    public static final String PROFIT_MARGIN_REQUIRED = "RECIPE_PROFIT_MARGIN_REQUIRED";
    public static final String PROFIT_MARGIN_NEGATIVE = "RECIPE_PROFIT_MARGIN_NEGATIVE";
    public static final String PROFIT_MARGIN_INVALID = "RECIPE_PROFIT_MARGIN_INVALID";
    public static final String VISIBLE_REQUIRED = "RECIPE_VISIBLE_REQUIRED";
    public static final String ACTIVE_REQUIRED = "RECIPE_ACTIVE_REQUIRED";

    @NotBlank(message = NAME_REQUIRED)
    @Size(min = 3, max = 150, message = NAME_LENGTH_INVALID)
    @Pattern(regexp = "^[a-zA-Z0-9ÁÉÍÓÚáéíóúñÑ ]+$", message = NAME_FORMAT_INVALID)
    private String name;

    @NotNull(message = CATEGORY_REQUIRED)
    private UUID categoryId;

    @Valid
    @NotNull(message = PRODUCTS_REQUIRED)
    @Size(min = 1, message = PRODUCTS_EMPTY)
    private List<
            @NotNull(message = PRODUCT_ITEM_NULL)
                    RecipeProductRequest
            > products;

    @Valid
    private List<
            @NotNull(message = SUBRECIPE_ITEM_NULL)
                    RecipeSubRecipeRequest
            > subRecipes;

    @NotNull(message = PROFIT_MARGIN_REQUIRED)
    @PositiveOrZero(message = PROFIT_MARGIN_NEGATIVE)
    @Max(
            value = 100,
            message = PROFIT_MARGIN_INVALID
    )
    private Integer profitMargin;


    @NotNull(message = VISIBLE_REQUIRED)
    private Boolean visibleInMenu;


    @NotNull(message = ACTIVE_REQUIRED)
    private Boolean active;
}