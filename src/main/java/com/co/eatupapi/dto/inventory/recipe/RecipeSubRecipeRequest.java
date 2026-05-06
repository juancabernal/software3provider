package com.co.eatupapi.dto.inventory.recipe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecipeSubRecipeRequest {

    public static final String SUBRECIPE_ID_REQUIRED = "RECIPE_SUBRECIPE_ID_REQUIRED";
    public static final String SUBRECIPE_QUANTITY_REQUIRED = "RECIPE_SUBRECIPE_QUANTITY_REQUIRED";
    public static final String SUBRECIPE_QUANTITY_INVALID = "RECIPE_SUBRECIPE_QUANTITY_INVALID";

    @NotNull(message = SUBRECIPE_ID_REQUIRED)
    private UUID subRecipeId;

    @NotNull(message = SUBRECIPE_QUANTITY_REQUIRED)
    @Positive(message = SUBRECIPE_QUANTITY_INVALID)
    private Integer quantity;
}