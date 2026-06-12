# SocialNetwork — Plataforma Social de Entretenimiento

Monolito modular (Spring Modulith) para tracking social de videojuegos, películas, series, libros, cómics, manga y anime. Documentación de diseño en [docs/](docs/).

## Estado: Fase 0 — Fundaciones ✅

Esqueleto del backend con fronteras de módulos verificadas, entorno local reproducible con Docker Compose y CI.

## Requisitos

- Java 21 (Temurin recomendado)
- Maven 3.9+ (o ejecutar `mvn wrapper:wrapper` una vez para añadir `./mvnw` al repo)
- Docker + Docker Compose

## Arranque en local

```bash
# 1. Infraestructura (Postgres, Redis, Mailpit)
docker compose up -d

# 2. Backend con perfil dev
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Verificación: <http://localhost:8080/actuator/health> debe responder `{"status":"UP"}`.

| Servicio | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Health | http://localhost:8080/actuator/health |
| Mailpit (emails de dev) | http://localhost:8025 |

Perfiles opcionales de Compose: `--profile search` (OpenSearch, post-beta) y `--profile app` (la app en contenedor).

## Tests y verificación de arquitectura

```bash
cd backend
mvn verify
```

Ejecuta: compilación con Error Prone, tests (incl. integración con Testcontainers — requiere Docker), verificación de fronteras de módulos (Spring Modulith), reglas de Clean Architecture (ArchUnit) y formato (Spotless). Para formatear: `mvn spotless:apply`.

## Estructura

```
backend/src/main/java/com/socialnetwork/
├── auth/        # fase 1 — autenticación
├── user/        # fase 2 — perfiles
├── catalog/     # fase 3 — catálogo multimedia
├── search/      # fase 3 — búsqueda (Postgres FTS tras SearchPort)
├── tracking/    # fase 4 — listas
├── review/      # fase 5 — reviews
├── social/      # fase 6 — follows, posts, comentarios
├── feed/        # fase 6 — timeline (solo consume eventos)
└── shared/      # kernel compartido: errores, config transversal
```

Cada módulo sigue Clean Architecture: `api/` (contratos públicos + eventos), `domain/`, `application/`, `infrastructure/`, `web/`. Las fronteras se verifican en CI; violarlas rompe la build.
