# Frontend Handoff MVP (Mobile-First)

Objetivo: integrar frontend rápido sobre `master` sin romper contrato backend validado.

## 1) Contrato frontend-consumable (estable)

Headers recomendados en todas las llamadas:
- `Content-Type: application/json`
- `X-API-Version: 1` (opcional, pero recomendado fijarlo desde frontend)
- `Authorization: Bearer <google_id_token>` cuando `APP_SECURITY_REQUIRE_AUTH=true`

### Ingredients
- `GET /api/ingredients?q=`
  - uso: autocomplete de ingredientes por `id/name/alias`
- `POST /api/ingredients/custom`
  - body:
```json
{
  "name": "Carne de conejo",
  "measurementType": "WEIGHT"
}
```

Respuesta `IngredientResponse`:
```json
{
  "id": "rice",
  "name": "Arroz",
  "measurementType": "WEIGHT",
  "allowedUnits": ["GRAM", "KILOGRAM"],
  "custom": false
}
```

### Recipes
- `POST /api/recipes`
- `GET /api/recipes`
- `GET /api/recipes?type=DINNER`
- `GET /api/recipes/{id}`
- `PUT /api/recipes/{id}`
- `DELETE /api/recipes/{id}`

Body create/update:
```json
{
  "name": "Arroz con huevo",
  "type": "DINNER",
  "ingredients": [
    { "ingredientId": "rice", "quantity": 200, "unit": "GRAM" },
    { "ingredientId": "egg", "quantity": 2, "unit": "PIECE" }
  ],
  "preparation": "Mezclar y cocinar",
  "notes": "Cena rápida",
  "tags": ["rapido", "batch"]
}
```

Respuesta `RecipeResponse` incluye:
- `id, name, type, ingredients, preparation, notes, tags`
- `usageCount, lastUsedAt, createdAt, updatedAt`

### Meal Plans
- `POST /api/plans`
- `GET /api/plans`
- `GET /api/plans/{id}`
- `PUT /api/plans/{id}`
- `DELETE /api/plans/{id}`

Body create/update:
```json
{
  "startDate": "2026-02-09",
  "period": "WEEK",
  "slots": [
    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "<recipe-id>" }
  ]
}
```

Respuesta `MealPlanResponse`:
- `id, startDate, endDate, period, slots, createdAt, updatedAt`

### Shopping List Draft
- `POST /api/shopping-lists/generate?planId={planId}`
  - header opcional: `Idempotency-Key: <key>`
- `GET /api/shopping-lists`
- `GET /api/shopping-lists/{id}`
- `PUT /api/shopping-lists/{id}` (replace completo de `items`)
- `DELETE /api/shopping-lists/{id}`

Respuesta `ShoppingListResponse`:
```json
{
  "id": "<draft-id>",
  "planId": "<plan-id>",
  "items": [
    {
      "id": "<item-id>",
      "ingredientId": "rice",
      "name": "Arroz",
      "quantity": 1000,
      "unit": "GRAM",
      "suggestedPackages": 1,
      "packageAmount": 1.0,
      "packageUnit": "KILOGRAM",
      "manual": false,
      "bought": false,
      "note": null,
      "sortOrder": 0
    }
  ],
  "createdAt": "2026-02-08T00:00:00Z",
  "updatedAt": "2026-02-08T00:00:00Z"
}
```

Body update (`PUT /api/shopping-lists/{id}`):
- enviar siempre lista completa final de `items`.
- `sortOrder` define el orden persistido.
- para item manual usar `manual=true` y `ingredientId` puede ser `null`.
- para item no manual, `ingredientId` es obligatorio.

## 2) Flujo UX MVP (mobile-first) sin romper contrato

1. Recetas:
- lista de recetas (cards compactas)
- CTA flotante `Nueva receta`
- formulario con autocomplete de ingredientes + validación de unidad

2. Plan:
- pantalla de plan activo (`WEEK/FORTNIGHT`) con tabs por día
- cada slot (`BREAKFAST/LUNCH/DINNER`) permite seleccionar receta existente
- guardar plan como operación explícita (`POST`/`PUT`)

3. Shopping:
- botón `Generar lista` desde detalle de plan
- checklist editable por item:
  - toggle comprado (`bought`)
  - nota (`note`)
  - reordenar (drag handles / mover arriba-abajo)
- botón `Guardar cambios` (envía `PUT` con reemplazo completo)

## 3) Plan de integración frontend

### Auth strategy
- Modo local/dev rápido:
  - backend con `APP_SECURITY_REQUIRE_AUTH=false`
  - frontend no envía bearer token
- Modo real:
  - login Google en frontend
  - guardar `id_token` en memoria (y opcional sessionStorage)
  - enviar `Authorization: Bearer <id_token>` en `/api/**`
- Ambos modos comparten mismo cliente API; sólo cambia un interceptor de auth.

### Manejo de errores por `ApiError.code`
Mapeo mínimo recomendado:
- `UNAUTHORIZED` -> pantalla/login requerido
- `FORBIDDEN` -> toast de permisos
- `RESOURCE_NOT_FOUND` -> estado vacío o redirección
- `VALIDATION_ERROR`, `INVALID_TYPE` -> errores inline de formulario
- `INGREDIENT_NOT_FOUND`, `INVALID_INGREDIENT_UNIT` -> feedback en receta
- `PLAN_RECIPE_NOT_FOUND`, `PLAN_SLOT_OUT_OF_RANGE`, `PLAN_DUPLICATE_SLOT` -> feedback en plan
- `SHOPPING_ITEM_*` -> feedback por item en shopping
- `UNSUPPORTED_API_VERSION` -> fallback técnico (forzar `X-API-Version: 1`)
- `INTERNAL_ERROR` -> toast genérico + retry

### Estado shopping list draft editable
Estrategia simple y robusta:
- `serverDraft` (último payload del backend)
- `draftItems` (estado editable local)
- `dirty` = diff simple entre `draftItems` y `serverDraft.items`
- edición local inmediata (optimista local)
- persistencia sólo al `Guardar`:
  - normalizar `sortOrder` secuencial antes de `PUT`
  - enviar `items` completos
  - al éxito: reemplazar `serverDraft` y limpiar `dirty`
- si falla `PUT`, conservar edición local y mostrar error por `code`

## 4) Checklist demo end-to-end

1. Login
- modo sin auth: backend en `APP_SECURITY_REQUIRE_AUTH=false`
- modo real: login Google y token válido (audience = `GOOGLE_CLIENT_ID`)

2. Crear receta
- crear receta con 2+ ingredientes válidos
- validar que aparece en listado (`GET /api/recipes`)

3. Crear plan
- crear plan `WEEK` con slots válidos
- validar errores de negocio si slot duplicado o receta inexistente

4. Generar shopping list
- `POST /api/shopping-lists/generate?planId=...`
- repetir con mismo `Idempotency-Key` y validar idempotencia

5. Editar/ordenar/marcar comprados
- marcar `bought=true` en varios items
- agregar `note`
- reordenar items (`sortOrder`)
- guardar (`PUT`) y verificar respuesta

6. Persistencia y recarga
- recargar app
- abrir draft por id (`GET /api/shopping-lists/{id}`)
- validar que `bought/note/sortOrder` persisten

## 5) Endpoints menores: criterio

No se requieren endpoints nuevos para el MVP actual.

Opcional futuro (no bloqueante): `PATCH /api/shopping-lists/{id}/items/{itemId}` para evitar `PUT` completo, pero hoy introduce complejidad innecesaria.

## Wireframes Mobile-First en Figma (next steps)

## Principios de layout móvil
- Frame base: `390x844` (iPhone 13/14), grid 4 columnas, margen 16.
- Navegación: bottom nav de 3 tabs (`Recetas`, `Plan`, `Lista`).
- Patrón base: header compacto + contenido scrolleable + CTA fijo inferior.

## Pantallas mínimas (low-fidelity -> high-fidelity)
1. `AuthGate` (solo cuando auth activo)
- estado: no autenticado
- CTA: `Continuar con Google`

2. `RecipesList`
- buscador + filtro `MealType`
- cards de receta (nombre, tipo, tags, usage)
- FAB `+`

3. `RecipeForm`
- nombre, tipo, ingredientes dinámicos
- autocomplete de ingrediente (`/api/ingredients?q=`)
- selector de unidad restringido por `allowedUnits`

4. `PlanEditor`
- selector periodo (`WEEK/FORTNIGHT`)
- carrusel/tab por día
- slots breakfast/lunch/dinner con picker de receta
- CTA `Guardar plan` y `Generar lista`

5. `ShoppingListDraft`
- checklist por item (bought)
- edición inline de `note`
- control de orden (drag o botones mover)
- CTA `Guardar cambios`

6. `ErrorStates`
- componente de error toast/banner con mapping por `ApiError.code`

## Organización recomendada en Figma
- Página `00_Foundations`:
  - colores, tipografía, spacing, radios, elevación
  - componentes base: button, input, chip, toast, list-item
- Página `01_Wireframes_Mobile`:
  - flujo completo en low-fi
- Página `02_UI_Mobile`:
  - UI final con componentes
- Página `03_Prototypes`:
  - prototipo click-through de demo E2E

### Setup inicial (hoy)
1. Crear archivo Figma: `AppCompras_MVP_Mobile_v1`.
2. Crear exactamente estas páginas:
- `00_Foundations`
- `01_Wireframes_Mobile`
- `02_UI_Mobile`
- `03_Prototypes`
3. Crear página separada para inspiración visual:
- `99_Style_References` (solo referencia, fuera del flujo MVP)
- pegar aquí los frames/ideas del archivo `Untitled`:
  - [Referencia estilo](https://www.figma.com/design/D7R92H10osrXPzH3becTal/Untitled?node-id=0-1&p=f&t=kn1FXnmdtS5aoD7X-0)
4. Regla de uso:
- `99_Style_References` no entra al prototipo ni al handoff técnico.
- solo usar como guía de look&feel (tipografía, color, ritmo visual).
5. Configurar frame base móvil:
- `390x844`
- grid 4 columnas
- margen horizontal `16`
- spacing base `8`
6. Convención de nombres de frames:
- `MVP-Mobile/AuthGate`
- `MVP-Mobile/RecipesList`
- `MVP-Mobile/RecipeForm`
- `MVP-Mobile/PlanEditor`
- `MVP-Mobile/ShoppingListDraft`
- `MVP-Mobile/ErrorStates`

## Blueprint wireframes (6 pantallas MVP)

### 1) `AuthGate`
- Header simple con logo/nombre.
- Mensaje corto: autenticación requerida.
- CTA primario: `Continuar con Google`.
- Link secundario opcional (solo dev): `Entrar sin login`.
- Estado alterno: error auth (`UNAUTHORIZED`) con banner.

### 2) `RecipesList`
- App bar con título `Recetas`.
- Campo búsqueda (`name=q`).
- Filtro rápido por tipo: chips `BREAKFAST/LUNCH/DINNER`.
- Lista de cards:
  - nombre
  - meal type
  - tags
  - usageCount
- FAB inferior derecha: `+ Nueva receta`.
- Estado vacío: `Aún no hay recetas`.

### 3) `RecipeForm`
- Campos:
  - `name` (texto)
  - `type` (selector enum)
  - `ingredients[]` (repetible)
  - `preparation` (textarea)
  - `notes` (textarea)
  - `tags` (chips/text)
- Item de ingrediente:
  - autocomplete por `/api/ingredients?q=`
  - `quantity`
  - `unit` restringida por `allowedUnits`
- CTA sticky inferior:
  - primario `Guardar receta`
  - secundario `Cancelar`
- Estado error inline: `INGREDIENT_NOT_FOUND`, `INVALID_INGREDIENT_UNIT`, `VALIDATION_ERROR`.

### 4) `PlanEditor`
- Encabezado con rango de fechas (`startDate - endDate`).
- Selector de periodo: `WEEK | FORTNIGHT`.
- Navegación por días (tabs/carrusel horizontal).
- Por cada día: 3 slots fijos:
  - `BREAKFAST`
  - `LUNCH`
  - `DINNER`
- Cada slot abre picker de receta (`GET /api/recipes`).
- CTAs:
  - `Guardar plan`
  - `Generar lista`
- Estado error inline: `PLAN_RECIPE_NOT_FOUND`, `PLAN_SLOT_OUT_OF_RANGE`, `PLAN_DUPLICATE_SLOT`.

### 5) `ShoppingListDraft`
- Header con referencia de plan.
- Lista ordenable de items (visual drag handle o flechas subir/bajar).
- Cada item:
  - checkbox/toggle `bought`
  - `name + quantity + unit`
  - metadata de empaque (`suggestedPackages`, `packageAmount`, `packageUnit`) cuando exista
  - campo `note`
- CTA sticky: `Guardar cambios`.
- Confirmación de éxito: toast `Cambios guardados`.
- Estado error por item: `SHOPPING_ITEM_*`.

### 6) `ErrorStates`
- Librería de overlays y banners reutilizables:
  - `UNAUTHORIZED` (bloqueante con CTA login)
  - `FORBIDDEN`
  - `RESOURCE_NOT_FOUND`
  - `VALIDATION_ERROR / INVALID_TYPE`
  - `INTERNAL_ERROR`
  - `UNSUPPORTED_API_VERSION`
- Definir variante visual:
  - inline (form)
  - banner (pantalla)
  - toast (acción breve)

## Secuencia de trabajo en Figma (rápida)
1. Wireframes low-fi del flujo completo (60-90 min).
2. Validación funcional contra contrato API (15 min).
3. Crear componentes base reusables (45 min).
4. Pasar a high-fi sólo de pantallas demo (60 min).
5. Prototipo navegable + edge states (30 min).
6. Handoff: specs de spacing, estados, y copy final.

## Plan de arranque (ejecución inmediata)
1. Mover/duplicar referencias al page `99_Style_References`.
2. Crear en `01_Wireframes_Mobile` los 6 frames base (solo bloques, sin detalle visual):
- `AuthGate`
- `RecipesList`
- `RecipeForm`
- `PlanEditor`
- `ShoppingListDraft`
- `ErrorStates`
3. Conectar navegación mínima en `03_Prototypes`:
- `AuthGate -> RecipesList -> RecipeForm -> RecipesList -> PlanEditor -> ShoppingListDraft`
4. Checkpoint rápido:
- confirmar que cada CTA principal tiene endpoint asociado del contrato backend.
5. Recién después pasar a `02_UI_Mobile` para estilizar usando la referencia visual.

## Conexiones de prototipo (03_Prototypes)
1. `AuthGate -> RecipesList` (login OK).
2. `RecipesList -> RecipeForm` (tap FAB).
3. `RecipeForm -> RecipesList` (guardar éxito).
4. `RecipesList -> PlanEditor` (tab Plan).
5. `PlanEditor -> ShoppingListDraft` (tap `Generar lista`).
6. `ShoppingListDraft -> ShoppingListDraft` (guardar -> estado éxito).
7. Desde cualquier pantalla -> `ErrorStates` (overlays de error para demo).

## Handoff a implementación frontend
- Entregar links por pantalla con nombre estable:
  - `MVP-Mobile/RecipesList`
  - `MVP-Mobile/RecipeForm`
  - `MVP-Mobile/PlanEditor`
  - `MVP-Mobile/ShoppingListDraft`
- Adjuntar tabla `UI event -> endpoint` en ticket de integración.
- Congelar versión v1 del diseño antes de empezar código.
