package com.co.eatupapi.services.inventory.recipe;

import com.co.eatupapi.domain.inventory.recipe.RecipeDomain;
import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import com.co.eatupapi.dto.inventory.recipe.RecipeSubRecipeRequest;
import com.co.eatupapi.messaging.inventory.recipe.RecipeCreateEventPublisher;
import com.co.eatupapi.repositories.inventory.recipe.RecipeRepository;
import com.co.eatupapi.utils.inventory.recipe.exceptions.ErrorCode;
import com.co.eatupapi.utils.inventory.recipe.exceptions.RecipeBusinessException;
import com.co.eatupapi.utils.inventory.recipe.mapper.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreateRecipeService {

    public static final String RECIPE_WITH_NAME_EXISTS = "La receta con el nombre %s ya existe.";

    private final RecipeRepository repo;
    private final RecipeMapper mapper;
    private final GenerateRecipeIdService idService;
    private final RecipeValidatorService recipeValidator;
    private final RecipeExistenceValidatorService existenceValidator;
    private final CalculateRecipeCostService costService;
    private final CalculateRecipeSellingPriceService sellingPriceService;
    private final RecipeCreateEventPublisher publisher;

    public CreateRecipeService(
            RecipeRepository repo,
            RecipeMapper mapper,
            GenerateRecipeIdService idService,
            RecipeValidatorService recipeValidator,
            RecipeExistenceValidatorService existenceValidator,
            CalculateRecipeCostService costService,
            CalculateRecipeSellingPriceService sellingPriceService,
            RecipeCreateEventPublisher publisher
    ) {
        this.repo = repo;
        this.mapper = mapper;
        this.idService = idService;
        this.recipeValidator = recipeValidator;
        this.existenceValidator = existenceValidator;
        this.costService = costService;
        this.sellingPriceService = sellingPriceService;
        this.publisher = publisher;
    }

    @Transactional
    public void run(RecipeRequest request) {

        validatePreviousExistence(request.getName());
        validateSubRecipesIfPresent(request);

        UUID id = idService.run();

        RecipeDomain recipe = mapper.toNewDomain(request, id);

        if (recipe.getSubRecipes() == null) {
            recipe.setSubRecipes(List.of());
        }

        BigDecimal baseCost = costService.run(request);

        BigDecimal sellingPrice = sellingPriceService.run(
                baseCost,
                request.getProfitMargin()
        );

        recipe.setBaseCost(baseCost);
        recipe.setSellingPrice(sellingPrice);

        recipeValidator.validate(recipe);

        repo.save(recipe);

        publisher.publish(request);
    }

    private void validatePreviousExistence(String name) {

        if (repo.existsByName(name)) {

            throw new RecipeBusinessException(
                    ErrorCode.RECIPE_ALREADY_EXISTS,
                    String.format(RECIPE_WITH_NAME_EXISTS, name)
            );
        }
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
}