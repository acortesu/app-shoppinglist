package com.appcompras.recipe;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create recipe")
    public RecipeResponse createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        Recipe recipe = recipeService.create(request);
        return RecipeResponse.from(recipe);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recipe by id")
    public RecipeResponse getRecipeById(@PathVariable String id) {
        Recipe recipe = recipeService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return RecipeResponse.from(recipe);
    }

    @GetMapping
    @Operation(summary = "List recipes")
    public List<RecipeResponse> getRecipes(
            @Parameter(description = "Optional meal type filter", example = "DINNER")
            @RequestParam(required = false) MealType type
    ) {
        return recipeService.findAll(type).stream()
                .map(RecipeResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recipe")
    public RecipeResponse updateRecipe(@PathVariable String id, @Valid @RequestBody CreateRecipeRequest request) {
        Recipe recipe = recipeService.update(id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return RecipeResponse.from(recipe);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete recipe")
    public void deleteRecipe(@PathVariable String id) {
        boolean deleted = recipeService.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }
    }
}
