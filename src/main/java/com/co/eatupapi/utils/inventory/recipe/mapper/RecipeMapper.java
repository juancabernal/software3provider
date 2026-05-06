package com.co.eatupapi.utils.inventory.recipe.mapper;

import com.co.eatupapi.domain.inventory.recipe.RecipeDomain;
import com.co.eatupapi.domain.inventory.recipe.RecipeProduct;
import com.co.eatupapi.domain.inventory.recipe.RecipeSubRecipe;
import com.co.eatupapi.dto.inventory.recipe.RecipeProductRequest;
import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import com.co.eatupapi.dto.inventory.recipe.RecipeResponse;
import com.co.eatupapi.dto.inventory.recipe.RecipeSubRecipeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        imports = {
                UUID.class,
                LocalDateTime.class
        }
)
public interface RecipeMapper {

    RecipeResponse toResponse(RecipeDomain recipe);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "products", source = "request.products")
    @Mapping(target = "subRecipes", source = "request.subRecipes")
    @Mapping(target = "locationId", expression = "java(UUID.fromString(\"11111111-1111-1111-1111-111111111111\"))")
    @Mapping(target = "baseCost", ignore = true)
    @Mapping(target = "sellingPrice", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    RecipeDomain toNewDomain(RecipeRequest request, UUID id);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "locationId", ignore = true)
    @Mapping(target = "products", source = "request.products")
    @Mapping(target = "subRecipes", source = "request.subRecipes")
    @Mapping(target = "baseCost", ignore = true)
    @Mapping(target = "sellingPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    void toUpdatedDomain(RecipeRequest request, @MappingTarget RecipeDomain existingRecipe);

    default RecipeProduct map(RecipeProductRequest request) {

        if (request == null) {
            return null;
        }

        RecipeProduct product = new RecipeProduct();
        product.setProductId(request.getProductId());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());

        return product;
    }

    default List<RecipeProduct> map(List<RecipeProductRequest> products) {

        if (products == null) {
            return List.of();
        }

        return products.stream()
                .map(this::map)
                .toList();
    }

    default RecipeSubRecipe map(RecipeSubRecipeRequest request) {

        if (request == null) {
            return null;
        }

        RecipeSubRecipe sub = new RecipeSubRecipe();
        sub.setSubRecipeId(request.getSubRecipeId());
        sub.setQuantity(request.getQuantity());

        return sub;
    }

    default List<RecipeSubRecipe> mapSubRecipes(List<RecipeSubRecipeRequest> subs) {

        if (subs == null) {
            return List.of();
        }

        return subs.stream()
                .map(this::map)
                .toList();
    }
}