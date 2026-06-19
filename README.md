# SocialNetwork — Plataforma Social de Entretenimiento

Monolito modular (Spring Modulith) para tracking social de videojuegos, películas, series, libros, cómics, manga y anime. Documentación de diseño en [docs/](docs/).

## Estado

- **Fase 0 — Fundaciones ✅** Esqueleto Spring Modulith con fronteras verificadas (ArchUnit), Docker Compose, CI, Problem Details (RFC 9457), logging estructurado y OpenAPI.
- **Fase 1 — Autenticación 🔄** Registro, verificación de email (Mailpit), login con JWT en cookies httpOnly, refresh rotativo con detección de reuso por familia (Redis), logout/logout global y rate limiting por IP. Añadido: **recuperación de contraseña**, **OAuth2 Google** y **MFA TOTP opcional**. Pendiente de cierre de fase: revisión OWASP final.

Endpoints en `/api/auth` (detalle en Swagger UI):

- Cuenta: `register`, `verify-email`, `resend-verification`, `forgot-password`, `reset-password`.
- Sesión: `login`, `login/mfa`, `refresh`, `logout`, `logout-all`, `me`.
- MFA (autenticado): `mfa/enroll`, `mfa/confirm`, `mfa/disable`, `mfa/status`.
- OAuth2: iniciar en `GET /oauth2/authorization/google`; Google redirige a `/login/oauth2/code/google`.

### Login social con Google (opcional)

Deshabilitado salvo que se definan credenciales. Crea un OAuth Client ID en Google Cloud (URI de redirección `http://localhost:8080/login/oauth2/code/google` en dev) y exporta antes de arrancar el backend las variables canónicas de Spring (el `registration-id` `google` es un proveedor conocido: el resto —endpoints y scopes— lo rellena Spring):

```bash
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
```

Spring activa el flujo automáticamente cuando detecta el registro; sin estas variables la app arranca como resource-server puro. (Alternativa: descomentar el bloque `spring.security.oauth2` de `application.yml`.)

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
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"   # las comillas son necesarias en PowerShell
```

Flujo rápido de prueba: registra un usuario en Swagger UI → el email de verificación llega a Mailpit (http://localhost:8025) → `POST /api/auth/verify-email` con el token del enlace → `POST /api/auth/login`.

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
