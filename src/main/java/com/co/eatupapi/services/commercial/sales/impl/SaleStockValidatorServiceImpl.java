package com.co.eatupapi.services.commercial.sales.impl;

import com.co.eatupapi.dto.commercial.sales.SaleDetailDTO;
import com.co.eatupapi.dto.inventory.product.ProductDTO;
import com.co.eatupapi.dto.inventory.recipe.RecipeResponse;
import com.co.eatupapi.services.commercial.sales.SaleStockValidatorService;
import com.co.eatupapi.services.inventory.product.ProductService;
import com.co.eatupapi.services.inventory.recipe.GetRecipeByIdService;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleBusinessException;
import com.co.eatupapi.utils.commercial.sales.exceptions.SaleValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleStockValidatorServiceImpl implements SaleStockValidatorService {

    private static final String TEMPORARY_STOCK_VALIDATION_NOTE =
            "Validación temporal: mientras recetas no exponga cantidad requerida por producto, "
                    + "sales valida únicamente que cada producto asociado tenga stock mayor que cero.";

    private final GetRecipeByIdService getRecipeByIdService;
    private final ProductService productService;

    public SaleStockValidatorServiceImpl(GetRecipeByIdService getRecipeByIdService,
                                         ProductService productService) {
        this.getRecipeByIdService = getRecipeByIdService;
        this.productService = productService;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateStockForSaleDetails(List<SaleDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            throw new SaleValidationException("La venta debe tener al menos una línea de detalle.");
        }

        Set<UUID> productIdsToValidate = new HashSet<>();
        Set<UUID> resolvedRecipes = new HashSet<>();

        for (SaleDetailDTO detail : details) {
            if (detail == null) {
                throw new SaleValidationException("Cada línea de detalle es obligatoria.");
            }

            if (detail.getRecipeId() == null) {
                throw new SaleValidationException("El recipeId es obligatorio en cada línea.");
            }

            collectProductsFromRecipe(
                    detail.getRecipeId(),
                    new HashSet<>(),
                    resolvedRecipes,
                    productIdsToValidate
            );
        }

        validateProductsHaveStock(productIdsToValidate);
    }

    private void collectProductsFromRecipe(UUID recipeId,
                                           Set<UUID> currentPath,
                                           Set<UUID> resolvedRecipes,
                                           Set<UUID> productIdsToValidate) {
        if (resolvedRecipes.contains(recipeId)) {
            return;
        }

        if (currentPath.contains(recipeId)) {
            throw new SaleBusinessException("Se detectó una referencia circular entre recetas. RecipeId: " + recipeId);
        }

        currentPath.add(recipeId);

        RecipeResponse recipe = getRecipeOrThrowAsSaleBusinessException(recipeId);
        validateRecipeCanBeSold(recipe);

        addRecipeProducts(recipe, productIdsToValidate);
        resolveSubRecipes(recipe, currentPath, resolvedRecipes, productIdsToValidate);

        currentPath.remove(recipeId);
        resolvedRecipes.add(recipeId);
    }

    private RecipeResponse getRecipeOrThrowAsSaleBusinessException(UUID recipeId) {
        try {
            return getRecipeByIdService.run(recipeId);
        } catch (RuntimeException exception) {
            throw new SaleBusinessException("No se puede vender la receta con id " + recipeId
                    + " porque no existe o no pudo ser consultada.");
        }
    }

    private void validateRecipeCanBeSold(RecipeResponse recipe) {
        if (recipe == null) {
            throw new SaleBusinessException("No se puede vender una receta inexistente.");
        }

        if (recipe.getId() == null) {
            throw new SaleBusinessException("No se puede vender una receta sin id.");
        }

        if (!Boolean.TRUE.equals(recipe.getActive())) {
            throw new SaleBusinessException("No se puede vender la receta '" + recipe.getName()
                    + "' porque no está activa.");
        }
    }

    private void addRecipeProducts(RecipeResponse recipe, Set<UUID> productIdsToValidate) {

        if (recipe.getProducts() == null || recipe.getProducts().isEmpty()) {
            return;
        }

        for (var product : recipe.getProducts()) {

            if (product == null || product.getProductId() == null) {
                throw new SaleBusinessException("La receta '" + recipe.getName()
                        + "' tiene un producto inválido.");
            }

            productIdsToValidate.add(product.getProductId());
        }
    }

    private void resolveSubRecipes(RecipeResponse recipe,
                                   Set<UUID> currentPath,
                                   Set<UUID> resolvedRecipes,
                                   Set<UUID> productIdsToValidate) {

        if (recipe.getSubRecipes() == null || recipe.getSubRecipes().isEmpty()) {
            return;
        }

        recipe.getSubRecipes().forEach(sub -> {

            if (sub == null || sub.getSubRecipeId() == null) {
                throw new SaleBusinessException("La receta '" + recipe.getName()
                        + "' tiene una subreceta inválida.");
            }

            collectProductsFromRecipe(
                    sub.getSubRecipeId(),
                    currentPath,
                    resolvedRecipes,
                    productIdsToValidate
            );
        });
    }

    private void validateProductsHaveStock(Set<UUID> productIdsToValidate) {
        if (productIdsToValidate.isEmpty()) {
            throw new SaleBusinessException("No se puede validar stock porque las recetas no tienen productos asociados.");
        }

        for (UUID productId : productIdsToValidate) {
            ProductDTO product = getProductOrThrowAsSaleBusinessException(productId);
            validateProductHasStock(product);
        }
    }

    private ProductDTO getProductOrThrowAsSaleBusinessException(UUID productId) {
        try {
            return productService.findById(productId);
        } catch (RuntimeException exception) {
            throw new SaleBusinessException("No se puede vender porque el producto con id "
                    + productId + " no existe o no pudo ser consultado.");
        }
    }

    private void validateProductHasStock(ProductDTO product) {
        if (product == null) {
            throw new SaleBusinessException("No se puede vender porque uno de los productos no existe.");
        }

        if (product.getStock() == null) {
            throw new SaleBusinessException("No se puede vender el producto '" + product.getName()
                    + "' porque no tiene stock registrado.");
        }

        if (product.getStock().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SaleBusinessException("No se puede vender el producto '" + product.getName()
                    + "' porque no tiene stock disponible. " + TEMPORARY_STOCK_VALIDATION_NOTE);
        }
    }
}