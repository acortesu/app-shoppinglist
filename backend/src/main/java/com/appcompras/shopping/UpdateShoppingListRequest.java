package com.appcompras.shopping;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateShoppingListRequest(
        @NotNull List<@Valid ItemInput> items
) {

    public record ItemInput(
            @Schema(example = "9c5f8ca2-f8d1-4ad5-b0a0-8ebea5c9f6ac")
            String id,
            @Schema(example = "rice")
            String ingredientId,
            @Schema(example = "Rice")
            @NotBlank String name,
            @Schema(example = "500")
            @NotNull @Positive Double quantity,
            @Schema(example = "GRAM")
            @NotBlank String unit,
            @Schema(example = "1")
            Integer suggestedPackages,
            @Schema(example = "1.0")
            Double packageAmount,
            @Schema(example = "KILOGRAM")
            String packageUnit,
            @Schema(example = "false")
            @NotNull Boolean manual,
            @Schema(example = "true")
            Boolean bought,
            @Schema(example = "Comprar marca integral")
            String note,
            @Schema(example = "1")
            Integer sortOrder
    ) {
    }
}
