# Agentic QA Planning Assistant — Backend ⚙️

Complete A-Z reference for the Spring Boot backend service — covering project setup, architecture, all environment variables, database schema, developer auth, AI integration, and deployment on Render.

---

## 📑 Table of Contents

1. [What This Backend Does](#-what-this-backend-does)
2. [Full Project Flow A-Z](#-full-project-flow-a-z)
3. [Tech Stack](#-tech-stack)
4. [Project Package Structure](#-project-package-structure)
5. [Developer Auth](#-developer-credentials--auth)
6. [JWT Authentication Flow](#-jwt-authentication-flow)
7. [Database Schema (All Tables)](#-database-schema-all-tables)
8. [AI Service — Groq API Integration](#-ai-service--groq-api-integration)
9. [Environment Variables (Full Reference)](#-environment-variables-full-reference)
10. [application.yml Configuration](#-applicationyml-configuration)
11. [API Endpoints Reference](#-api-endpoints-reference)
12. [Deploying on Render](#-deploying-on-render)
13. [Running Locally](#-running-locally)

---

## 🎯 What This Backend Does

The backend is a **Spring Boot 3 REST API** that:
- Authenticates developers and issues JWT tokens.
- Retrieves enterprise QA guidelines via a RAG knowledge base stored in `classpath:knowledge-base/`.
- Calls **Groq API** (LLM: `llama-3.3-70b-versatile`) to generate structured JSON QA test plans.
- Saves plans, test cases, and acceptance criteria to **Supabase PostgreSQL** via JPA/Hibernate.
- Runs deterministic post-processing: duplicate detection, incomplete test flagging, coverage calculation.
- Generates enterprise-quality downloadable PDF documents using **OpenPDF**.

---

## 🔄 Full Project Flow A-Z

```
1. Developer opens https://agenticqa.barathraj.in (Vercel frontend)
2. Developer enters name + password → POST /api/auth/login
3. Backend validates against in-memory DEVELOPERS map (Barath/Rishabh/Whiskey)
4. Backend generates a signed HMAC-SHA256 JWT (24hr expiry) and returns it
5. Frontend stores JWT in localStorage → attaches as "Authorization: Bearer <token>" to all API calls

6. Developer fills in:  Title, Description, Requirement, Implementation Summary, Acceptance Criteria (AC1..N)
7. Frontend → POST /api/qa/generate  (with JWT header)

8. JwtAuthFilter intercepts request → validates JWT → extracts developerName → attaches to request
9. QaPlanService calls KnowledgeBaseService → keyword search across QA guidelines text files
10. QaPlanService calls QaAiService → builds system prompt + user prompt → calls Groq API
    Model: llama-3.3-70b-versatile
    Temperature: 0.3 (deterministic, low creativity)

11. Groq API returns JSON → QaAiService.parseAiResponse() → Jackson ObjectMapper maps to AiGeneratedPlan DTO

12. QaPlanService saves to PostgreSQL:
    - qa_plan (main entity with developerName, title, requirement, etc.)
    - acceptance_criteria (each AC line as a row linked to plan)
    - generated_test_case (each test case row linked to plan)
    - test_case_mapping (junction: each test case linked to its mapped ACs)

13. CoverageService computes: coveragePercentage = (distinct ACs mapped by test cases / total ACs) * 100
14. DuplicateDetectionService flags similar test case titles
15. IncompleteTestDetectionService flags missing fields (steps/expectedResult/preconditions)

16. Response with full QaPlan JSON returned to frontend
17. Developer reviews, approves/rejects individual test cases → PUT /api/qa/testcase/{id}/approve or /reject
18. Developer downloads PDF → GET /api/qa/{id}/pdf → PdfExportService generates and streams PDF binary
```

---

## 🛠 Tech Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.4.1 |
| Language | Java 17 |
| Database ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Supabase) |
| AI Integration | Spring AI ChatClient (OpenAI-compatible API) |
| LLM Provider | **Groq API** (`llama-3.3-70b-versatile`) |
| Security | JJWT (Java JWT) — HMAC-SHA256 |
| PDF Export | OpenPDF |
| Boilerplate Reduction | Lombok |
| JSON Parsing | Jackson ObjectMapper |
| Containerization | Docker |
| Deployment | Render (Docker environment) |

---

## 📦 Project Package Structure

```
backend/
└── src/main/java/com/qaassistant/
    ├── QaPlanningAssistantApplication.java  ← Spring Boot entry point
    ├── config/
    │   ├── JwtAuthFilter.java               ← JWT validation on every request
    │   ├── JwtUtil.java                     ← Token generation & validation
    │   ├── SecurityConfig.java              ← Spring Security filter chain
    │   ├── VectorStoreConfig.java           ← RAG knowledge base loader
    │   └── WebConfig.java                   ← CORS configuration
    ├── controller/
    │   ├── AuthController.java              ← POST /api/auth/login
    │   └── QaPlanController.java            ← All /api/qa/** endpoints
    ├── dto/
    │   └── ai/
    │       └── AiGeneratedPlan.java         ← JSON mapping from Groq response
    ├── entity/
    │   ├── QaPlan.java                      ← Main JPA entity
    │   ├── AcceptanceCriteria.java          ← AC rows (FK → qa_plan)
    │   ├── GeneratedTestCase.java           ← Test case rows (FK → qa_plan)
    │   ├── TestCaseMapping.java             ← Junction table
    │   ├── QaPlanVersion.java               ← Version history
    │   ├── TestCaseStatus.java              ← Enum: PROPOSED/APPROVED/REJECTED
    │   ├── TestCategory.java                ← Enum: 10 test categories
    │   └── TestPriority.java                ← Enum: HIGH/MEDIUM/LOW
    ├── repository/
    │   └── QaPlanRepository.java            ← JPA queries (search, findByDeveloperName)
    ├── exception/
    │   └── GlobalExceptionHandler.java      ← Centralized error responses
    └── service/
        ├── QaAiService.java                 ← Groq API prompt + response parsing
        ├── QaPlanService.java               ← Core orchestration logic
        ├── KnowledgeBaseService.java        ← RAG retrieval from classpath files
        ├── CoverageService.java             ← AC coverage % calculation
        ├── DuplicateDetectionService.java   ← Similarity-based duplicate flagging
        ├── IncompleteTestDetectionService.java ← Missing field detection
        └── PdfExportService.java            ← Enterprise PDF generation
```

---

## 🔑 Developer Auth

Developer access is validated against a static in-memory map in `AuthController.java`. Each entry maps a developer name to their password.

> **To add a new developer**: Edit the `DEVELOPERS` map in `AuthController.java`, rebuild and redeploy.

Each developer's plans are **fully isolated** — every QA plan is stored with `developer_name` and all queries filter by the JWT's embedded developer identity.

---

## 🔐 JWT Authentication Flow

```
POST /api/auth/login
Body: { "developerName": "<your-name>", "password": "<your-password>" }

→ AuthController validates against DEVELOPERS map
→ JwtUtil.generateToken(developerName) — HMAC-SHA256, 24hr expiry
→ Response: { "token": "eyJ...", "developerName": "..." }

All subsequent requests:
Header: Authorization: Bearer eyJ...
→ JwtAuthFilter.doFilterInternal()
→ Validates signature + expiry
→ Extracts developerName from claims
→ Sets request.setAttribute("developerName", ...)
→ Controller reads this for data scoping
```

---

## 🗄 Database Schema (All Tables)

Database: **Supabase PostgreSQL** at `db.ascidgwzfjcwcmzkuxam.supabase.co:5432/postgres`
Hibernate DDL: `update` (auto-creates/updates tables on startup)

### Table: `qa_plan`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `developer_name` | VARCHAR(255) | Owner (Barath/Rishabh/Whiskey) |
| `title` | VARCHAR(255) | Plan title |
| `description` | TEXT | Scope description |
| `requirement` | TEXT | User story / feature requirement |
| `implementation_summary` | TEXT | Dev implementation notes |
| `user_flows` | TEXT | JSON array of flow step strings |
| `retrieved_guidance` | TEXT | RAG-retrieved QA guidelines |
| `assumptions` | TEXT | JSON array of assumption strings |
| `coverage_percentage` | DOUBLE PRECISION | Calculated AC coverage (0–100) |
| `current_version` | INT | Version counter (starts at 1) |
| `created_date` | TIMESTAMP | Creation time |
| `updated_date` | TIMESTAMP | Last update time |

### Table: `acceptance_criteria`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `plan_id` | BIGINT FK | References `qa_plan(id)` |
| `criteria_index` | INT | 1-based: AC1=1, AC2=2, ... |
| `description` | TEXT | Criterion text |

### Table: `generated_test_case`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `plan_id` | BIGINT FK | References `qa_plan(id)` |
| `test_id` | VARCHAR(50) | e.g. `TC-001`, `TC-002` |
| `title` | VARCHAR(255) | Test case title |
| `category` | VARCHAR(50) | Enum: UNIT_TESTS, API_TESTS, INTEGRATION_TESTS, END_TO_END_TESTS, PLAYWRIGHT_TESTS, MANUAL_TESTS, EDGE_CASES, PERMISSION_CASES, FAILURE_STATES, REGRESSION_AREAS |
| `preconditions` | TEXT | Setup required before test |
| `steps` | TEXT | Step-by-step execution |
| `expected_result` | TEXT | What should happen |
| `priority` | VARCHAR(20) | HIGH / MEDIUM / LOW |
| `status` | VARCHAR(30) | PROPOSED / APPROVED / REJECTED |
| `approved` | BOOLEAN | Developer approval flag |
| `reason` | TEXT | Why this test is needed |

### Table: `test_case_mapping`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `test_case_id` | BIGINT FK | References `generated_test_case(id)` |
| `acceptance_criteria_id` | BIGINT FK | References `acceptance_criteria(id)` |

---

## 🤖 AI Service — Groq API Integration

The AI integration uses **Spring AI** with the OpenAI-compatible interface, configured to point to Groq's API.

### Model Used
```
Provider: Groq (https://console.groq.com)
Model:    llama-3.3-70b-versatile
Temp:     0.3 (low randomness = consistent outputs)
```

### How Groq is Configured (application.yml)
```yaml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/
      chat:
        options:
          model: llama-3.3-70b-versatile
          temperature: 0.3
```

Spring AI's OpenAI client works seamlessly with Groq because Groq exposes an **OpenAI-compatible REST API**. No special library needed.

### Getting a Groq API Key
1. Go to https://console.groq.com
2. Sign up / log in
3. Click **API Keys** → **Create API Key**
4. Copy the key starting with `gsk_...`
5. Set as `GROQ_API_KEY` environment variable on Render

### System Prompt (Exact)
```
You are a QA Planning Assistant. Your role is to PROPOSE test cases for developer review.

CRITICAL RULES:
- NEVER state that a feature has passed QA, is approved, or is ready for release.
- NEVER make final quality judgments. Only propose test cases.
- Generate userFlows, testCases, edgeCases, permissionCases, failureStates, regressionAreas, and assumptions.
- Every test case MUST include: testId, title, category, preconditions, steps, expectedResult,
  priority (HIGH/MEDIUM/LOW), reason, mappedAcceptanceCriteria (1-based indices).
- Map each test case to one or more acceptance criteria by index.

Valid categories: UNIT_TESTS, API_TESTS, INTEGRATION_TESTS, END_TO_END_TESTS,
PLAYWRIGHT_TESTS, MANUAL_TESTS, EDGE_CASES, PERMISSION_CASES, FAILURE_STATES, REGRESSION_AREAS

Respond ONLY with valid JSON.
```

---

## ⚙️ Environment Variables (Full Reference)

### Set these on Render Dashboard → Environment tab:

| Variable | Example Value | Description |
|---|---|---|
| `PORT` | `8080` | Server port (Render sets this automatically) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` | Supabase DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Supabase DB username |
| `SPRING_DATASOURCE_PASSWORD` | `<your-supabase-password>` | Supabase DB password |
| `GROQ_API_KEY` | `gsk_xxxxxxxxxxxxxxxxxxxx` | Groq API key from console.groq.com |
| `AI_BASE_URL` | `https://api.groq.com/openai/` | Groq OpenAI-compatible base URL |
| `AI_MODEL` | `llama-3.3-70b-versatile` | LLM model name |
| `CORS_ALLOWED_ORIGINS` | `https://yourdomain.com,https://your-app.vercel.app,http://localhost:5173` | All allowed frontend origins (comma-separated, no spaces) |

---

## 📄 application.yml Configuration

```yaml
spring:
  application:
    name: qa-planning-assistant
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://db.ascidgwzfjcwcmzkuxam.supabase.co:5432/postgres?sslmode=require}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:YOUR_SUPABASE_PASSWORD_HERE}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  ai:
    openai:
      api-key: ${GROQ_API_KEY:YOUR_GROQ_API_KEY_HERE}
      base-url: ${AI_BASE_URL:https://api.groq.com/openai/}
      chat:
        options:
          model: ${AI_MODEL:llama-3.3-70b-versatile}
          temperature: 0.3

server:
  port: ${PORT:8080}

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,https://agentic-qa-planner-frontend.vercel.app,https://agenticqa.barathraj.in}
  knowledge-base:
    path: classpath:knowledge-base/
  rag:
    top-k: 5
```

---

## 🌐 API Endpoints Reference

All protected endpoints require: `Authorization: Bearer <token>`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | ❌ Public | Login with developerName + password → returns JWT |
| `POST` | `/api/qa/generate` | ✅ JWT | Generate + save new AI QA plan |
| `GET` | `/api/qa` | ✅ JWT | List developer's saved QA plans (optional `?search=keyword`) |
| `GET` | `/api/qa/{id}` | ✅ JWT | Get full QA plan details by ID |
| `DELETE` | `/api/qa/{id}` | ✅ JWT | Delete developer's QA plan |
| `GET` | `/api/qa/{id}/pdf` | ✅ JWT | Download enterprise PDF of QA plan |
| `PUT` | `/api/qa/testcase/{id}/approve` | ✅ JWT | Mark test case as APPROVED |
| `PUT` | `/api/qa/testcase/{id}/reject` | ✅ JWT | Mark test case as REJECTED |

---

## 🚀 Deploying on Render

1. Push backend code to `https://github.com/barathrex/agentic-qa-planner-backend.git`
2. Go to https://render.com → **New** → **Web Service**
3. Connect the `agentic-qa-planner-backend` GitHub repository
4. Configure:
   - **Environment**: Docker
   - **Branch**: `main`
   - **Docker Command**: auto-detected from `Dockerfile`
5. Add all environment variables listed above in the **Environment** tab
6. Click **Deploy**

> ⚠️ First deploy can take 5–10 minutes as Maven downloads all dependencies.

---

## 💻 Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL connection (or use Supabase directly)

### Steps
```bash
# Clone the repo
git clone https://github.com/barathrex/agentic-qa-planner-backend.git
cd agentic-qa-planner-backend

# Set environment variables (or edit application.yml defaults)
export SPRING_DATASOURCE_PASSWORD=<your-supabase-password>
export GROQ_API_KEY=<your-groq-api-key>

# Run
mvn spring-boot:run
```

Server starts at `http://localhost:8080`
