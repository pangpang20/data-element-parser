# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Data Element Parser (数据元解析与治理规则生成服务) — a Chinese government data governance tool that parses natural-language data element descriptions into structured metadata (conforming to Jiangsu province standard DB32/T 4124) and generates OceanBase-compatible SQL governance rules.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Maven (single dependency: spring-boot-starter-web)
- **Frontend:** Vue 3 (Composition API, `<script setup>`), Vite 5.4+
- **No database** — all data is in-memory (hardcoded template list in `DataElementParserService`)

## Common Commands

```bash
# Start both services (backend :8081, frontend :3000)
./start.sh

# Stop both services
./stop.sh

# Backend only
cd backend && mvn spring-boot:run

# Frontend only (after npm install)
cd frontend && npm run dev

# Production build
cd backend && mvn clean package        # produces JAR
cd frontend && npm run build           # outputs to frontend/dist/
```

There are no tests or linting configurations in the project.

## Architecture

### Backend (Spring Boot, package `com.datagov`)

Layered architecture under `backend/src/main/java/com/datagov/`:

- **Controller** (`controller/DataElementController.java`) — REST endpoints at `/api`:
  - `POST /api/parse` — input `{ input: "..." }` → returns `DataElement[]`
  - `POST /api/generate-rules` — input `{ dataElements: [...] }` → returns `GovernanceRule[]`
  - `GET /api/health` — health check
- **Services:**
  - `DataElementParserService` — core parsing logic. Contains ~100 hardcoded `DataElementTemplate` entries across 13 categories. Parses by splitting on delimiters, extracting length/type hints via regex, fuzzy-matching against templates using character-overlap scoring.
  - `RuleGeneratorService` — generates SQL rules per element: not-null, length, precision, value-range, and format checks (REGEXP patterns for IDs, phones, plates, etc.). SQL expressions use `{0}` as column-name placeholder.
- **Models** — POJOs: `DataElement` (GB/T 19488.1-2004 属性体系：标识类/定义类/表示类/关系类/管理类), `GovernanceRule` (含六性标签 qualityDimension + regexExpression/javaCode/pythonCode), `ParseRequest`, `RuleRequest`
- **Config** — `CorsConfig.java` (wide-open CORS), `application.yml` (port 8081)

### Frontend (Vue 3 SPA, no router, no state management)

Entire UI lives in `frontend/src/App.vue` — textarea input → parse → results table → generate rules → rule cards with SQL highlighting. Styles in `frontend/src/style.css`. Vite proxies `/api` to `localhost:8081` in dev.

## Key Design Decisions

- Parsing uses fuzzy character-overlap matching against a hardcoded template list, not an external NLP service
- Data element model conforms to GB/T 19488.1-2004 (标识类/定义类/表示类/关系类/管理类五大属性组)
- Each governance rule is tagged with one of six quality dimensions (六性): 唯一性/完整性/准确性/一致性/时效性/规范性
- Each rule generates four implementations: OceanBase SQL, regex, Java code, Python code
- Column placeholder `{0}` in SQL expressions is meant to be replaced by the consumer with actual column names
- The template database in `DataElementParserService` is the central knowledge base — adding new data element types means adding entries there
