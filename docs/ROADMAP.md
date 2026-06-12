# Roadmap del MVP

> Cada fase termina con tests (unitarios + integración con Testcontainers), migraciones Flyway y endpoints documentados (OpenAPI). No se avanza de fase con deuda crítica.
>
> **Restricción económica: coste cero hasta beta local funcional.** Ningún servicio de pago (AWS, dominio, email transaccional, OpenSearch gestionado) hasta que el MVP completo funcione correctamente en local. Todo el desarrollo y la beta corren sobre Docker Compose.

## Fase 0 — Fundaciones ✅ (completada 2026-06-12)

- Esqueleto del monolito modular (Spring Modulith), estructura de carpetas por módulo.
- Docker Compose (Postgres, Redis, Mailpit; OpenSearch como perfil opcional, no requerido para el MVP), Flyway, perfiles de configuración.
- CI con GitHub Actions: build, tests, verificación de fronteras de módulos (ArchUnit), análisis estático.
- Manejo global de errores (RFC 9457, Problem Details), logging estructurado, OpenAPI.

**Hecho cuando:** `docker compose up` + arrancar la app deja un entorno funcional con healthcheck y CI en verde. ✅

## Fase 1 — Autenticación (`auth`) 🔄 en curso

- [x] Registro + verificación de email (vía Mailpit en dev, sin proveedor de pago), login, JWT access (cookie httpOnly) + refresh rotativo con detección de reuso por familia (Redis), logout y logout global.
- [x] Rate limiting en endpoints sensibles (por IP, Redis).
- [ ] Recuperación de contraseña (reutiliza `email_tokens` con `type=RESET`).
- [ ] OAuth2 Google.
- [ ] MFA TOTP opcional (puede deslizarse a fase 7 si bloquea).

**Hecho cuando:** flujo completo registro→verificación→login→refresh→logout cubierto por tests de integración ✅ (`AuthFlowIntegrationTest`); revisión OWASP de los endpoints (al cerrar la fase).

## Frontend web — en paralelo desde el fin de la Fase 1

El frontend (Next.js + TypeScript + Tailwind + shadcn/ui + TanStack Query + Zustand) no espera al final del MVP: cada fase de backend se cierra con su slice vertical de UI, para validar la API (DTOs, paginación, errores) cuando aún es barato corregirla.

**Prerrequisito UX (ligero, antes del primer componente):**

- Arquitectura de información: navegación principal y jerarquía de pantallas (ficha de medio, biblioteca, perfil, feed).
- Wireframes low-fi de los 5–6 flujos clave (Figma/Excalidraw), con Letterboxd, Trakt y Goodreads como referentes.
- Design tokens (color, tipografía, espaciado) directamente como tema de Tailwind + shadcn/ui. Sin mockups high-fidelity ni design system propio.

| Tras fase backend | Slice de frontend |
|---|---|
| 1 — Auth | Setup del proyecto, tema, layout base; registro, login, verificación, recuperación de contraseña |
| 2 — Usuarios | Perfil público y edición (avatar, banner, bio), estadísticas |
| 3 — Catálogo | Búsqueda y ficha de medio |
| 4 — Listas | Biblioteca por tipo de medio, listas personalizadas |
| 5 — Reviews | Rating y reseñas en la ficha de medio (spoilers ocultos por defecto) |
| 6 — Social | Feed, posts, comentarios, follows |

**Hecho cuando** (por slice): la funcionalidad de la fase es usable end-to-end desde el navegador contra el backend local.

## Fase 2 — Usuarios (`user`)

- Perfil: avatar y banner tras `StoragePort` (filesystem local hasta producción; S3 después), bio, visibilidad.
- Historial de actividad (tabla append-only alimentada por eventos desde el día 1).
- Estadísticas básicas del perfil (cacheadas).

**Hecho cuando:** perfil público consultable, edición segura (solo dueño), imágenes con validación de tipo/tamaño.

## Fase 3 — Catálogo (`catalog` + `search`)

- Modelo unificado `media_items` + extensiones por tipo.
- Puerto `MediaMetadataProvider` + adaptadores: **TMDB e IGDB primero** (pelis/series/juegos cubren el grueso del uso); Google Books/Open Library, Jikan y Comic Vine después dentro de la misma fase.
- Import-on-demand con caché Redis y rate limiting por proveedor. Todas las APIs externas usadas tienen tier gratuito (IGDB vía credenciales Twitch, TMDB no comercial, Google Books, Open Library, Jikan, Comic Vine).
- Búsqueda: **Postgres FTS + trigram** tras el puerto `SearchPort`, indexación por eventos. OpenSearch queda como adaptador alternativo post-beta (sin coste ni RAM extra mientras tanto).

**Hecho cuando:** buscar un medio inexistente localmente lo importa y lo sirve; caída del proveedor externo no rompe el catálogo local.

## Fase 4 — Listas (`tracking`)

- Estados (pendiente/en progreso/completado/abandonado), favoritos, progreso por tipo de medio.
- Listas personalizadas públicas/privadas con orden manual.
- Eventos de actividad para el futuro feed.

**Hecho cuando:** un usuario gestiona su biblioteca completa por tipo de medio y las listas públicas son visibles en su perfil.

## Fase 5 — Reviews (`review`)

- Rating 1–10 + reseña, marca de spoilers (oculta por defecto en UI), likes.
- Recalculo asíncrono de `avg_rating` en catálogo.

**Hecho cuando:** página de un medio muestra rating agregado y reseñas paginadas; una review por usuario y medio.

## Fase 6 — Social + Feed (`social`, `feed`)

- Follows, posts (con medio adjunto opcional), comentarios anidados (1 nivel), likes.
- Feed fan-out-on-read con paginación por cursor.

**Hecho cuando:** el feed muestra posts y actividad de seguidos con latencia aceptable; contadores consistentes.

➡️ **Fin del MVP = Beta local.** Todo el stack (backend + frontend web) funcionando en Docker Compose, con datos de prueba (seeds), flujos principales cubiertos por tests e2e y sin deuda crítica. **Sin gasto alguno hasta aquí.** Si hace falta feedback de testers externos, exponer la instancia local con un túnel gratuito (p. ej. Cloudflare Tunnel) antes que contratar hosting.

## Post-beta

| Fase | Alcance |
|---|---|
| 7 — Notificaciones | In-app por eventos, WS para tiempo real, preferencias de usuario |
| 8 — Chat | Conversaciones directas, STOMP/WS, presencia y read receipts en Redis |
| 9 — App Flutter | Offline-first: SQLite, cola de sync, resolución de conflictos. (La API ya es compatible: paginación por cursor + `updated_at` en DTOs) |
| 10 — Despliegue en producción | **Primera fase con gasto, solo tras beta validada.** Terraform (escrito ahora, no antes), instancia única + RDS, dominio, TLS, backups, email transaccional real (SES/Resend), S3 para ficheros |
| 11 — Recomendaciones | Basadas en contenido (géneros/ratings) primero; colaborativas después |
| 12 — Escalado | OpenSearch (cambio de adaptador en `SearchPort`), extracción de `feed`/`notification`/`chat` a servicios, Kafka/SQS, EKS, feed híbrido fan-out-on-write |

## Riesgos principales

- **Términos de uso de APIs externas** (IGDB requiere credenciales Twitch; TMDB prohíbe uso comercial sin acuerdo): revisar antes de lanzar públicamente.
- **Alcance**: chat y notificaciones fuera del MVP deliberadamente; resistir la tentación de adelantarlos.
- **Gasto prematuro**: la regla es coste cero hasta beta validada. Riesgo mitigado por diseño: búsqueda en Postgres (no OpenSearch), Mailpit (no SES), filesystem (no S3), sin Terraform/AWS hasta la fase 10. Cualquier excepción debe justificarse explícitamente.
- **Coste en producción** (fase 10+): mantener una sola instancia hasta tener usuarios; OpenSearch gestionado es lo más caro — seguir con Postgres FTS en producción mientras el volumen lo permita.
