# Modelo de dominio (MVP)

## Recipe

Campos obligatorios:
- `id`
- `name`
- `type`: `breakfast | lunch | dinner`
- `ingredients[]`

`RecipeIngredient`:
- `ingredient_id`
- `quantity` (number)
- `unit` (válida para ese ingrediente)

Campos opcionales:
- `preparation`
- `notes`
- `tags[]`

Metadata:
- `usage_count`
- `last_used_at`
- `created_at`
- `updated_at`

## Ingredient Catalog

Tipos de medición:
- `WEIGHT` -> base interna `grams`
- `VOLUME` -> base interna `milliliters`
- `UNIT` -> base interna `unit`
- `TO_TASTE` -> no se suma automáticamente

El catálogo define:
- unidades permitidas por ingrediente
- reglas de conversión por ingrediente + unidad
- sugerencias de compra (ej. arroz paquete 1kg)

## Shopping List

Flujo:
1. tomar recetas del período
2. convertir ingredientes a unidad base
3. agrupar por ingrediente
4. sugerir compra (packaging común)
5. permitir edición libre del resultado
