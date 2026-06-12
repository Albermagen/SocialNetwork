-- Fase 0: línea base. Extensiones requeridas por el modelo de datos (ver docs/DATABASE.md).
-- citext  → username/email case-insensitive con unicidad correcta
-- pg_trgm → búsqueda con tolerancia a erratas (índices GIN trigram) durante el MVP
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
