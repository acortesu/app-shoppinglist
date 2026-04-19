# Domain model (MVP)

## Recipe

Required fields:
- `id`
- `name`
- `type`: `breakfast | lunch | dinner`
- `ingredients[]`

`RecipeIngredient`:
- `ingredient_id`
- `quantity` (number)
- `unit` (must be allowed for the ingredient)

Optional fields:
- `preparation`
- `notes`
- `tags[]`

Metadata:
- `usage_count`
- `last_used_at`
- `created_at`
- `updated_at`

## Ingredient Catalog

Measurement types:
- `WEIGHT` -> internal base `grams`
- `VOLUME` -> internal base `milliliters`
- `UNIT` -> internal base `unit`
- `TO_TASTE` -> not aggregated automatically

The catalog defines:
- allowed units per ingredient
- conversion rules per ingredient + unit
- shopping suggestions (e.g. rice 1kg pack)

## Shopping List

Flow:
1. collect recipes for the period
2. convert ingredients to the base unit
3. group by ingredient
4. suggest purchase (common packaging)
5. allow free editing of the result
