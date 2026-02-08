package com.appcompras.shopping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateShoppingListRequest(
        @NotNull List<@Valid ItemInput> items
) {

    public record ItemInput(
            String id,
            String ingredientId,
            @NotBlank String name,
            @NotNull @Positive Double quantity,
            @NotBlank String unit,
            Integer suggestedPackages,
            Double packageAmount,
            String packageUnit,
            @NotNull Boolean manual,
            Boolean bought,
            String note,
            Integer sortOrder
    ) {
    }
}
