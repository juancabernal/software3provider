package com.co.eatupapi.repositories.inventory.recipe;

import com.co.eatupapi.domain.inventory.recipe.RecipeDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<RecipeDomain, UUID> {

    boolean existsByName(String name);

    Optional<RecipeDomain> findByName(String name);

    Optional<RecipeDomain> findByNameAndActiveTrue(String name);

    List<RecipeDomain> findBySubRecipes_SubRecipeId(UUID subRecipeId);
}