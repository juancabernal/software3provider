package com.co.eatupapi.services.inventory.recipe;

import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculateProductsCostService {

    public BigDecimal run(RecipeRequest request) {

        return request.getProducts()
                .stream()
                .map(p ->
                        p.getPrice().multiply(
                                BigDecimal.valueOf(p.getQuantity())
                        )
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}