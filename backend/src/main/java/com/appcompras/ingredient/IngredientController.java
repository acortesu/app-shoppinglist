package com.appcompras.ingredient;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.service.IngredientCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@Tag(name = "Ingredients")
public class IngredientController {

    private final IngredientCatalogService ingredientCatalogService;

    public IngredientController(IngredientCatalogService ingredientCatalogService) {
        this.ingredientCatalogService = ingredientCatalogService;
    }

    @GetMapping
    @Operation(summary = "List ingredient catalog")
    public List<IngredientResponse> listIngredients(
            @Parameter(description = "Optional search text by id/name/alias", example = "arroz")
            @RequestParam(required = false) String q
    ) {
        return ingredientCatalogService.list(q).stream()
                .map(IngredientResponse::from)
                .toList();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create custom ingredient")
    public IngredientResponse createCustom(@Valid @RequestBody CreateCustomIngredientRequest request) {
        IngredientCatalogItem item = ingredientCatalogService.createCustomIngredient(
                request.name(),
                request.measurementType()
        );
        return IngredientResponse.from(item);
    }
}
