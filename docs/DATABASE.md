# Modelo de Datos — PostgreSQL

> Convenciones: UUID v7 como PK (`id`), `created_at`/`updated_at` en todas las tablas, soft delete solo donde se indica, snake_case, Flyway para migraciones. Cada módulo posee sus tablas; no hay FKs entre módulos distintos salvo a `users.id` y `media_items.id` (referencias de identidad estables).

## 1. Módulos `auth` + `user`

```
users
├── id, username (unique, citext), email (unique, citext)
├── password_hash (nullable si solo OAuth), email_verified bool
├── role (USER|MODERATOR|ADMIN), status (ACTIVE|SUSPENDED|DELETED)
└── created_at, updated_at

oauth_identities          user_profiles                  mfa_credentials
├── id PK                 ├── user_id PK/FK              ├── user_id PK/FK
├── user_id FK            ├── display_name, bio          ├── secret (Base32; cifrado pendiente)
├── provider (GOOGLE)     ├── avatar_url, banner_url     ├── enabled bool
├── provider_user_id      ├── location, website          └── created_at, confirmed_at
├── email                 └── visibility (PUBLIC|PRIVATE)
└── UNIQUE(provider,                                     mfa_recovery_codes
    provider_user_id)                                    ├── id PK, user_id FK
                                                         ├── code_hash UNIQUE, used_at
email_tokens (verificación + reset password)             └── created_at
├── user_id FK, token_hash, type (VERIFY|RESET), expires_at, used_at, created_at
```

> Estado de implementación (fase 1): `users`, `email_tokens`, `oauth_identities`, `mfa_credentials` y `mfa_recovery_codes` ya existen (migraciones `V2`–`V4`). `user_profiles` llega en la fase 2. Códigos de recuperación MFA: tabla propia con hash y un solo uso (no array). Mejora pendiente (fase 10): cifrar `mfa_credentials.secret` en reposo.

Refresh tokens: **en Redis**, no en Postgres (`auth:rt:{hash}` → userId+familia, TTL). El reto MFA entre los dos pasos del login también vive en Redis (`auth:mfa:{hash}` → userId, TTL corto).

`user_activity` (historial): tabla append-only `(id, user_id, type, target_type, target_id, occurred_at)`, particionada por mes cuando crezca. Las estadísticas del perfil se calculan con vistas materializadas o contadores cacheados en Redis, no en caliente.

## 2. Módulo `catalog` — catálogo unificado

Una tabla base con los campos comunes + una tabla de extensión por tipo (herencia *joined* a nivel SQL, sin herencia JPA — composición explícita):

```
media_items (base)
├── id, media_type (GAME|MOVIE|TV_SHOW|BOOK|COMIC|MANGA|ANIME)
├── title, original_title, slug (unique), description
├── cover_url, banner_url, release_date, status_lifecycle
├── external_source (IGDB|TMDB|GOOGLE_BOOKS|OPEN_LIBRARY|JIKAN|COMIC_VINE)
├── external_id, external_synced_at
├── avg_rating numeric(3,1), ratings_count int   (desnormalizado, actualizado por evento)
└── UNIQUE(external_source, external_id)

media_games          media_videos (movie/tv)     media_books (book/comic/manga)
├── media_item_id PK ├── media_item_id PK        ├── media_item_id PK
├── platforms[]      ├── runtime_min             ├── authors[]
├── developer        ├── seasons_count           ├── page_count
├── publisher        ├── episodes_count          ├── isbn
└── game_modes[]     └── director (creator en TV)  └── publisher

media_anime
├── media_item_id PK, episodes_count, studio, season, source_material

genres (id, name, slug)        media_genres (media_item_id, genre_id)
```

**Por qué una tabla base:** listas, reviews, búsqueda y feed operan sobre "un medio" de forma polimórfica. Con 7 tablas independientes, cada feature necesitaría 7 uniones o FKs polimórficas sin integridad. La base da FK real + queries uniformes; las extensiones evitan una tabla ancha llena de NULLs.

## 3. Módulo `tracking` — listas

```
list_entries (las 5 listas de estado son una sola tabla)
├── id, user_id FK, media_item_id FK
├── status (PLANNED|IN_PROGRESS|COMPLETED|DROPPED)
├── is_favorite bool
├── progress (int: episodios/páginas/horas según tipo), progress_unit
├── started_at, finished_at, times_revisited
└── UNIQUE(user_id, media_item_id)

custom_lists                       custom_list_items
├── id, user_id FK                 ├── custom_list_id FK
├── name, description              ├── media_item_id FK
├── visibility (PUBLIC|PRIVATE)    ├── position int
└── items_count (desnorm.)         └── note, added_at
```

Estado y favorito son ortogonales (algo puede estar "completado" y "favorito"), por eso `is_favorite` es columna y no un valor más del enum.

## 4. Módulo `review`

```
reviews
├── id, user_id FK, media_item_id FK
├── rating smallint (1–10), title, body
├── has_spoilers bool, likes_count (desnorm.)
├── visibility, deleted_at (soft delete)
└── UNIQUE(user_id, media_item_id)

review_likes (review_id, user_id, created_at, PK compuesta)
```

Al crear/editar/borrar una review se publica `ReviewRatedEvent` → `catalog` recalcula `avg_rating` de forma asíncrona.

## 5. Módulos `social` + `feed`

```
follows (follower_id, followee_id, created_at, PK compuesta)

posts
├── id, user_id FK, body, media_urls[]
├── attached_media_item_id FK nullable   (post sobre un medio del catálogo)
├── likes_count, comments_count (desnorm.)
└── deleted_at

comments (id, post_id FK, user_id FK, parent_id FK nullable, body, likes_count, deleted_at)
post_likes / comment_likes (PK compuesta como review_likes)
```

**Feed MVP: fan-out-on-read.** Query sobre posts de seguidos ordenada por `created_at` con índice `(user_id, created_at DESC)`, cursor-based pagination. Suficiente hasta decenas de miles de usuarios. Post-MVP: fan-out-on-write con Redis para timelines materializados de usuarios activos (modelo híbrido; los seguidores de cuentas masivas se resuelven on-read).

## 6. Post-MVP: `notification` + `chat` (esbozo)

```
notifications (id, recipient_id, type, actor_id, target_type, target_id, read_at, created_at)
conversations (id, type DIRECT, created_at)
conversation_participants (conversation_id, user_id, last_read_message_id)
messages (id, conversation_id, sender_id, body, created_at)  → particionar por conversación/fecha
```

## 7. Índices críticos

| Tabla | Índice | Para |
|---|---|---|
| media_items | `(media_type, release_date DESC)`, GIN trigram en `title`, GIN sobre `tsvector` (title + description) | exploración + búsqueda principal del MVP |
| list_entries | `(user_id, status)`, `(media_item_id)` | mis listas / quién lo tiene |
| reviews | `(media_item_id, created_at DESC)` | reviews de un medio |
| posts | `(user_id, created_at DESC)` | feed on-read |
| follows | `(followee_id)` | listado de seguidores |
| user_activity | `(user_id, occurred_at DESC)` | historial perfil |

**Búsqueda durante el MVP: Postgres** (FTS con `tsvector` + trigram para tolerancia a erratas), tras el puerto `SearchPort`. OpenSearch (índices `media`, `users`, sincronizados por eventos) se incorpora post-beta cambiando solo el adaptador — sin coste de infraestructura ni RAM extra en local mientras tanto.

## 8. Contadores desnormalizados

`likes_count`, `comments_count`, `ratings_count`, `avg_rating`, `items_count` se actualizan vía eventos de dominio (mismo proceso, transacción separada o `@TransactionalEventListener AFTER_COMMIT`). Job nocturno de reconciliación para corregir derivas. Nunca `COUNT(*)` en caliente en endpoints de listado.
