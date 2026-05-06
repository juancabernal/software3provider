package com.co.eatupapi.services.inventory.recipe;

import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculateSubRecipesCostService {

    private final GetRecipeByIdService getRecipeByIdService;

    public CalculateSubRecipesCostService(
            GetRecipeByIdService getRecipeByIdService
    ) {
        this.getRecipeByIdService = getRecipeByIdService;
    }

    public BigDecimal run(RecipeRequest request) {

        if (request.getSubRecipes() == null || request.getSubRecipes().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return request.getSubRecipes()
                .stream()
                .map(sub ->
                        getRecipeByIdService
                                .run(sub.getSubRecipeId())
                                .getBaseCost()
                                .multiply(BigDecimal.valueOf(sub.getQuantity()))
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}