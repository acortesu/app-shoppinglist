# Week Meal Prep + Shopping List

Proyecto portfolio DevOps con foco en un MVP usable para planificación de comidas y generación de lista de compras inteligente.

## Objetivo MVP

- CRUD de recetas
- Planificación semanal o quincenal (breakfast/lunch/dinner)
- Generación automática de shopping list (suma + normalización + conversión)
- Shopping list editable por el usuario

## Criterios de diseño

- Simplicidad > complejidad
- Backend con lógica de dominio fuerte y testeable
- Frontend guiado para que el usuario no piense en conversiones
- Infra declarativa con Terraform en AWS
- Sin microservicios, sin Kubernetes, sin serverless forzado

## Estructura del repositorio

- `frontend/`: React + Vite (build estático para S3 + CloudFront)
- `backend/`: Java 17 + Spring Boot REST API (stateless, dockerizable)
- `infra/`: Terraform para entornos `dev` y `prod`
- `docs/`: alcance funcional, modelo de dominio y plan de implementación

## Próximos hitos

1. Definir modelo de dominio backend y reglas de conversión.
2. Implementar API de recetas y planificación.
3. Implementar generación/edición de shopping list.
4. Montar frontend MVP.
5. Completar IaC y pipeline CI/CD.
