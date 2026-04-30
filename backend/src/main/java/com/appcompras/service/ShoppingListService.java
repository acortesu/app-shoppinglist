package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.Recipe;
import com.appcompras.domain.RecipeIngredient;
import com.appcompras.domain.ShoppingListItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingListService {

    private final IngredientCatalogService catalogService;
    private final UnitConversionService conversionService;

    public ShoppingListService(IngredientCatalogService catalogService, UnitConversionService conversionService) {
        this.catalogService = catalogService;
        this.conversionService = conversionService;
    }

    public List<ShoppingListItem> generateFromRecipes(List<Recipe> recipes) {
        Map<String, Double> totalsByIngredient = new HashMap<>();

        for (Recipe recipe : recipes) {
            for (RecipeIngredient ingredient : recipe.ingredients()) {
                double baseAmount = conversionService.toBaseAmount(
                        ingredient.ingredientId(), ingredient.quantity(), ingredient.unit());

                if (baseAmount <= 0) {
                    continue;
                }

                totalsByIngredient.merge(ingredient.ingredientId(), baseAmount, Double::sum);
            }
        }

        List<ShoppingListItem> result = new ArrayList<>();

        for (Map.Entry<String, Double> entry : totalsByIngredient.entrySet()) {
            IngredientCatalogItem catalogItem = catalogService.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown ingredient: " + entry.getKey()));

            double requiredBaseAmount = entry.getValue();
            double packageBaseAmount = conversionService.packageBaseAmount(
                    catalogItem.measurementType(),
                    catalogItem.suggestedPurchaseAmount(),
                    catalogItem.suggestedPurchaseUnit());

            int suggestedPackages = packageBaseAmount > 0 ? (int) Math.ceil(requiredBaseAmount / packageBaseAmount) : 0;

            result.add(new ShoppingListItem(
                    catalogItem.ingredientId(),
                    catalogItem.displayName(),
                    requiredBaseAmount,
                    conversionService.baseUnitFor(catalogItem.ingredientId()),
                    suggestedPackages,
                    catalogItem.suggestedPurchaseAmount(),
                    catalogItem.suggestedPurchaseUnit()
            ));
        }

        return result;
    }
}
