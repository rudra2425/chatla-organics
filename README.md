# Chatla Organics

Migration workspace for the WordPress site at `C:\Users\swami\Local Sites\chatlaorganics`.

## Direction

The first version uses a modular monolith:

- `backend`: Spring Boot REST API, Java 21, MySQL, Flyway
- `frontend`: React + Vite
- `database`: MySQL 8 locally and in production
- media: object storage or a CDN in production, with a local filesystem adapter during development

This keeps local development and deployment inexpensive. Catalog, orders, content, identity, and media remain separate application modules so they can become services later without starting with distributed-system overhead.

## Prerequisites

Install Java 21, Maven 3.9+, Node.js 22+, and npm. Docker is optional for MySQL; the existing Local site already provides MySQL on port `10005`.

## Planned commands

```powershell
cd backend
mvn spring-boot:run

cd ..\frontend
npm install
npm run dev
```

The API is designed for `http://localhost:8080` and the frontend for `http://localhost:5173`.

## Migration status

See [MIGRATION.md](MIGRATION.md). The WordPress site remains the source of truth until content, media, storefront, checkout, accounts, SEO redirects, and operational email are migrated and verified.
