package com.co.eatupapi.services.inventory.recipe;

import com.co.eatupapi.domain.inventory.recipe.RecipeDomain;
import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import com.co.eatupapi.dto.inventory.recipe.RecipeSubRecipeRequest;
import com.co.eatupapi.messaging.inventory.recipe.RecipeUpdateEventPublisher;
import com.co.eatupapi.repositories.inventory.recipe.RecipeRepository;
import com.co.eatupapi.utils.inventory.recipe.exceptions.RecipeNotFoundException;
import com.co.eatupapi.utils.inventory.recipe.mapper.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateRecipeService {

    public static final String RECIPE_NOT_FOUND = "La receta con el nombre %s no fue encontrada.";

    private final RecipeRepository repo;
    private final RecipeMapper mapper;
    private final RecipeValidatorService recipeValidator;
    private final RecipeExistenceValidatorService existenceValidator;
    private final CalculateRecipeCostService costService;
    private final CalculateRecipeSellingPriceService sellingPriceService;
    private final RecalculateDependentRecipesCostService recalculateDependentRecipesCostService;
    private final RecipeUpdateEventPublisher publisher;

    public UpdateRecipeService(
            RecipeRepository repo,
            RecipeMapper mapper,
            RecipeValidatorService recipeValidator,
            RecipeExistenceValidatorService existenceValidator,
            CalculateRecipeCostService costService,
            CalculateRecipeSellingPriceService sellingPriceService,
            RecalculateDependentRecipesCostService recalculateDependentRecipesCostService,
            RecipeUpdateEventPublisher publisher
    ) {
        this.repo = repo;
        this.mapper = mapper;
        this.recipeValidator = recipeValidator;
        this.existenceValidator = existenceValidator;
        this.costService = costService;
        this.sellingPriceService = sellingPriceService;
        this.recalculateDependentRecipesCostService = recalculateDependentRecipesCostService;
        this.publisher = publisher;
    }

    @Transactional
    public void run(RecipeRequest request) {

        validateSubRecipesIfPresent(request);

        RecipeDomain existingRecipe = getExistingRecipe(request.getName());

        mapper.toUpdatedDomain(request, existingRecipe);

        if (existingRecipe.getSubRecipes() == null) {
            existingRecipe.setSubRecipes(List.of());
        }

        BigDecimal baseCost = costService.run(request);

        BigDecimal sellingPrice = sellingPriceService.run(
                baseCost,
                request.getProfitMargin()
        );

        existingRecipe.setBaseCost(baseCost);
        existingRecipe.setSellingPrice(sellingPrice);

        recipeValidator.validate(existingRecipe);

        repo.save(existingRecipe);

        recalculateDependentRecipesCostService.run(
                existingRecipe.getId()
        );

        publisher.publish(request);
    }

    private void validateSubRecipesIfPresent(RecipeRequest request) {

        if (request.getSubRecipes() == null || request.getSubRecipes().isEmpty()) {
            return;
        }

        List<UUID> ids = request.getSubRecipes()
                .stream()
                .map(RecipeSubRecipeRequest::getSubRecipeId)
                .toList();

        existenceValidator.run(ids);
    }

    private RecipeDomain getExistingRecipe(String name) {

        return repo.findByName(name)
                .orElseThrow(() -> new RecipeNotFoundException(
                        String.format(RECIPE_NOT_FOUND, name)
                ));
    }
}