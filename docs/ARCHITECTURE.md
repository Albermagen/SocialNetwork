# Arquitectura — Plataforma Social de Entretenimiento

> Estado: Diseño aprobado · Última actualización: 2026-06-11

## 1. Decisión global: Monolito Modular

Un único despliegue Spring Boot organizado en **módulos por bounded context**, con fronteras estrictas que permiten extraer microservicios más adelante sin reescritura.

**Justificación** (desarrollador solo, MVP primero):

| Criterio | Monolito modular | Microservicios día 1 |
|---|---|---|
| Velocidad de desarrollo | Alta | Baja (infra, contratos, despliegues) |
| Coste de infraestructura | 1 servicio + BD | N servicios, broker, service mesh |
| Transacciones | ACID locales | Sagas, consistencia eventual |
| Migración futura | Posible si las fronteras son estrictas | — |

**Reglas que hacen la migración posible:**

1. Un módulo solo accede a otro a través de su **API pública** (interfaces en `api/`), nunca a sus entidades ni repositorios.
2. Comunicación asíncrona entre módulos vía **eventos de dominio** (Spring `ApplicationEventPublisher` ahora; broker como Kafka/SQS después, cambiando solo el publicador).
3. Cada módulo posee **sus propias tablas**; prohibidos los JOIN entre módulos en queries de negocio (se permite composición en capa de lectura/BFF).
4. Verificación automática de fronteras con **Spring Modulith** + ArchUnit en CI.

## 2. Diagrama de sistema

```
                    ┌─────────────┐   ┌─────────────┐
                    │  Next.js    │   │   Flutter   │
                    │  (web)      │   │  (móvil,    │
                    └──────┬──────┘   │  offline-   │
                           │          │  first)     │
                           │          └──────┬──────┘
                       HTTPS │ REST + WS     │
                    ┌──────▼─────────────────▼──────┐
                    │      NGINX (TLS, rate limit)  │
                    └──────────────┬────────────────┘
                    ┌──────────────▼────────────────┐
                    │   Spring Boot 3 (Java 21)     │
                    │ ┌──────┐ ┌──────┐ ┌─────────┐ │
                    │ │ auth │ │ user │ │ catalog │ │
                    │ ├──────┤ ├──────┤ ├─────────┤ │
                    │ │lists │ │review│ │ social  │ │
                    │ ├──────┤ ├──────┤ ├─────────┤ │
                    │ │ feed │ │notif*│ │  chat*  │ │
                    │ └──────┘ └──────┘ └─────────┘ │
                    │      (* = post-MVP)           │
                    └──┬─────────┬─────────┬────────┘
                  ┌────▼───┐ ┌───▼───┐ ┌───▼────────┐
                  │Postgres│ │ Redis │ │OpenSearch  │
                  │(+ FTS) │ │       │ │(post-beta) │
                  └────────┘ └───────┘ └────────────┘
                           │
              ┌────────────▼─────────────┐
              │ Proveedores externos     │
              │ IGDB·TMDB·GBooks·OpenLib │
              │ Jikan·Comic Vine         │
              └──────────────────────────┘
```

## 3. Módulos (bounded contexts)

| Módulo | Responsabilidad | Fase |
|---|---|---|
| `auth` | Registro, login, OAuth2, JWT + refresh, verificación email, recuperación contraseña, MFA | MVP-1 |
| `user` | Perfil (avatar, banner, bio), estadísticas, historial de actividad | MVP-2 |
| `catalog` | Catálogo unificado de medios (juegos, pelis, series, libros, cómics, manga, anime) + integración con APIs externas | MVP-3 |
| `tracking` | Listas: pendiente, en progreso, completado, favoritos, abandonado, personalizadas | MVP-4 |
| `review` | Valoraciones, reseñas, spoilers, likes de reseñas | MVP-5 |
| `social` | Follows, posts, comentarios, likes | MVP-6 |
| `feed` | Composición del timeline (lectura sobre eventos de `social`, `tracking`, `review`) | MVP-6 |
| `notification` | Notificaciones in-app y push | Post-MVP |
| `chat` | Mensajería privada en tiempo real, presencia, read receipts | Post-MVP |
| `search` | Búsqueda tras `SearchPort`: Postgres FTS en MVP, OpenSearch post-beta | MVP-3 (básico) |
| `shared` | Kernel compartido: tipos de valor comunes, errores, eventos base. Mínimo posible. | — |

### Dependencias permitidas entre módulos

```
auth ← user ← {social, tracking, review, chat}
catalog ← {tracking, review}
feed ← eventos de {social, tracking, review}   (solo asíncrono)
notification ← eventos de todos                (solo asíncrono)
search ← eventos de {catalog, user, social}    (solo asíncrono)
```

`feed`, `notification` y `search` **no son invocados por nadie de forma síncrona salvo por los controladores**: consumen eventos. Esto los hace los primeros candidatos a extraerse como microservicios.

## 4. Arquitectura interna de cada módulo (Clean Architecture)

```
backend/src/main/java/com/socialnetwork/
├── auth/
│   ├── api/              # Interfaces públicas para otros módulos + eventos publicados
│   ├── domain/           # Entidades, value objects, reglas de negocio (sin Spring)
│   ├── application/      # Casos de uso (services), puertos (interfaces), DTOs internos
│   ├── infrastructure/   # JPA, Redis, clientes externos, adaptadores (implementan puertos)
│   └── web/              # Controllers REST, DTOs request/response, mappers
├── user/ ...             # misma estructura
├── catalog/ ...
└── shared/
```

**Regla de dependencia:** `web → application → domain` y `infrastructure → application/domain`. El dominio no conoce Spring ni JPA. Los controladores nunca contienen lógica de negocio: delegan en casos de uso y mapean DTOs.

## 5. Catálogo: capa anticorrupción para APIs externas

Cada tipo de medio tiene un puerto y un adaptador sustituible:

```
application/port/ MediaMetadataProvider        (interfaz común)
infrastructure/provider/
  ├── IgdbGameProvider          → IGDB
  ├── TmdbMovieProvider         → TMDB (películas y series)
  ├── GoogleBooksProvider       → Google Books (fallback: Open Library)
  ├── JikanAnimeMangaProvider   → Jikan
  └── ComicVineProvider         → Comic Vine
```

Estrategia **import-on-demand**: cuando un usuario busca/añade un medio que no existe localmente, se importa desde el proveedor y se persiste como entidad propia (`media_item`) con `external_source` + `external_id`. La plataforma nunca depende en caliente de las APIs externas para servir su catálogo. Refresco periódico para metadatos volátiles (ratings, fechas de emisión). Rate limiting y caché Redis por proveedor.

## 6. Responsabilidades de cada tecnología

| Tecnología | Uso |
|---|---|
| PostgreSQL | Fuente de verdad. Una BD, esquemas/tablas por módulo. **También búsqueda full-text durante el MVP** (FTS + trigram) |
| Redis | Caché (metadatos externos, perfiles), sesiones de refresh token (revocación), rate limiting, presencia online (post-MVP), pub/sub WS multi-nodo (post-MVP) |
| OpenSearch | Búsqueda avanzada de medios y usuarios; alimentado por eventos. **Diferido a post-beta**: detrás del puerto `SearchPort`, en dev es un perfil opcional de Compose |
| WebSockets (STOMP) | Chat y notificaciones en tiempo real (post-MVP) |
| Flyway | Migraciones de BD versionadas |
| Mailpit (dev) | Servidor SMTP local para verificación de email y reset de contraseña sin proveedor de pago. En producción se cambia el adaptador (`MailPort`) a SES/Resend |
| Almacenamiento de ficheros | `StoragePort` con adaptador de **filesystem local** durante toda la fase sin gasto (avatares, banners). Adaptador S3/MinIO cuando haya despliegue |

**Principio transversal:** toda dependencia que en producción costaría dinero (búsqueda, email, almacenamiento de objetos, broker) queda detrás de un puerto con un adaptador gratuito/local. Pasar a la versión de pago es cambiar un adaptador y configuración, no rediseñar.

## 7. Seguridad (resumen; ver diseño por endpoint en cada módulo)

- **JWT access token** corto (15 min) + **refresh token** rotativo con detección de reuso, almacenado en Redis (revocable) y enviado como cookie `HttpOnly` `Secure` `SameSite=Strict` en web; almacenamiento seguro nativo en Flutter.
- OAuth2 (Google/GitHub) vía Spring Security `oauth2-client`, vinculado a cuenta local.
- Contraseñas con **Argon2id**. MFA TOTP opcional.
- OWASP: validación de entrada (Bean Validation), CORS restrictivo, headers de seguridad en NGINX, rate limiting por IP+usuario, protección contra enumeración de cuentas, paginación obligatoria.
- Autorización por método (`@PreAuthorize`) + comprobación de propiedad en casos de uso, nunca solo en el controlador.

## 8. Frontend y móvil (resumen)

- **Web (Next.js App Router):** TanStack Query como capa de servidor-estado (caché, revalidación), Zustand solo para estado UI global (modales, sesión). Componentes Shadcn/UI. SSR para páginas públicas de medios/perfiles (SEO), CSR para áreas autenticadas.
- **Flutter offline-first:** SQLite como réplica local; escrituras a cola de sincronización con `updated_at` + resolución last-write-wins por campo (conflictos detectados por versión); sync incremental por `cursor` al recuperar conexión. Diseño detallado en fase móvil.

## 9. Infraestructura por etapas

> **Restricción del proyecto: coste cero hasta tener una beta funcional en local.** No se contrata ningún servicio de pago (AWS, dominios, email transaccional) hasta que el MVP completo funcione correctamente en el entorno local.

| Etapa | Infra | Coste |
|---|---|---|
| Desarrollo y beta local (fases 0–6) | Docker Compose: app, Postgres, Redis, Mailpit; OpenSearch como perfil opcional. CI con GitHub Actions (gratis en repo público/minutos free tier). Búsqueda con Postgres FTS. Ficheros en filesystem local | 0 € |
| Validación de beta | La misma app accesible para testers: opción A, túnel gratuito (Cloudflare Tunnel) sobre la máquina local; opción B, free tier temporal. Solo si hace falta feedback externo | 0 € |
| Producción (solo tras beta validada) | 1 contenedor app + NGINX en instancia pequeña (Lightsail/EC2), RDS Postgres, dominio + TLS. **Terraform se escribe en esta etapa**, antes de crear el primer recurso de pago | bajo |
| Escalado | EKS (Kubernetes), réplicas app stateless, Redis pub/sub para WS, ElastiCache, OpenSearch gestionado, extracción de `feed`/`notification`/`chat` como servicios | según tráfico |

Para un desarrollador solo, Kubernetes se pospone: aporta coste y complejidad sin beneficio hasta tener tráfico real. Terraform también se pospone hasta la etapa de producción — escribir IaC para recursos que no existen aún es trabajo especulativo; lo que sí se mantiene desde el día 1 es que **todo el entorno sea reproducible con Docker Compose**, que cumple el mismo papel en local.

## 10. Decisiones registradas (ADR resumidas)

1. **Monolito modular con Spring Modulith** — migrable, verificable en CI.
2. **Catálogo unificado con herencia por tipo** (ver DATABASE.md §2) — evita 7 subsistemas duplicados.
3. **Import-on-demand de metadatos** — independencia de APIs externas en caliente.
4. **Eventos de dominio internos desde el día 1** — feed/notificaciones/search desacoplados; el broker llega después sin rediseño.
5. **Refresh tokens en Redis** — revocación inmediata, logout global, detección de robo.
6. **Coste cero hasta beta local funcional** — ningún servicio de pago hasta validar el MVP completo en local. Toda dependencia con coste futuro (búsqueda, email, almacenamiento, broker) se abstrae tras un puerto con adaptador gratuito: Postgres FTS en vez de OpenSearch, Mailpit en vez de SES, filesystem en vez de S3. Terraform se escribe justo antes del primer despliegue de pago, no antes.

## Documentos relacionados

- [DATABASE.md](DATABASE.md) — modelo de datos completo
- [ROADMAP.md](ROADMAP.md) — fases y criterios de finalización
