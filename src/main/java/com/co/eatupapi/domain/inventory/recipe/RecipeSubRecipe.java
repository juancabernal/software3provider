package com.co.eatupapi.domain.inventory.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Embeddable
public class RecipeSubRecipe {

    @NotNull(message = "RECIPE_SUBRECIPE_ID_NULL")
    @Column(name = "subrecipe_id", nullable = false)
    private UUID subRecipeId;

    @NotNull(message = "RECIPE_SUBRECIPE_QUANTITY_REQUIRED")
    @Positive(message = "RECIPE_SUBRECIPE_QUANTITY_INVALID")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}