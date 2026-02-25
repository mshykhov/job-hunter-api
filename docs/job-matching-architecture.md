# Job Matching Architecture

Design doc for multi-user job matching pipeline with AI-powered filtering.

## Current State

### What works
- `POST /jobs/ingest` — batch ingest from n8n, dedup by URL
- `GET /criteria?source={SOURCE}` — aggregated search criteria for n8n
- `JobEntity` with source, dedup, audit fields
- `UserPreferenceEntity` with categories, excluded keywords, disabled sources

### Problems to fix
1. **`JobEntity.status`** — status is per-job, but should be per user-job pair
2. **Ingest not atomic** — each job saved in separate transaction, partial failures possible
3. **N+1 queries in ingest** — separate `findByUrl()` per job in batch
4. **`description` nullable mismatch** — SQL allows NULL, entity says NOT NULL
5. **`company` not updated** on existing jobs (intentional — company is part of job identity)
6. **No user management** — no UserEntity, no preference CRUD
7. **No job-to-user matching** — jobs are stored but never matched to users

---

## Target Architecture

### Entity Model

```
┌──────────────┐       ┌──────────────────┐       ┌─────────────────┐
│   JobEntity  │       │  UserJobEntity   │       │   UserEntity    │
│              │ 1───N │  (per-user view) │ N───1 │                 │
│ id (UUID)    │       │                  │       │ id (UUID)       │
│ title        │       │ id (UUID)        │       │ auth0_sub (UK)  │
│ company      │       │ job_id (FK)      │       │ email           │
│ url (UK)     │       │ user_id (FK)     │       │ name            │
│ description  │       │ status (enum)    │       │ telegram_chat_id│
│ source       │       │ ai_relevance_score│      │ created_at      │
│ salary       │       │ ai_reasoning     │       │ updated_at      │
│ location     │       │ created_at       │       └────────┬────────┘
│ remote       │       │ updated_at       │                │
│ published_at │       └──────────────────┘                │ 1
│ matched_at   │                                           │
│ created_at   │                               ┌───────────┴────────┐
│ updated_at   │                               │ UserPreference     │
│              │                               │     Entity         │
│ (NO status!) │                               │                    │
└──────────────┘                               │ id (UUID)          │
                                               │ user_id (FK, UK)   │
                                               │ raw_input (TEXT)    │
                                               │ categories[]       │
                                               │ seniority_levels[] │
                                               │ keywords[]         │
                                               │ excluded_keywords[]│
                                               │ min_salary         │
                                               │ remote_only        │
                                               │ disabled_sources[] │
                                               │ notifications_on   │
                                               │ created_at         │
                                               │ updated_at         │
                                               └────────────────────┘
```

### Key changes from current model

| Entity | Change |
|--------|--------|
| `JobEntity` | Remove `status`, add `matched_at` |
| `UserPreferenceEntity` | Replace `user_sub` with `user_id` FK, add `raw_input`, `seniority_levels[]`, `keywords[]`, `min_salary` |
| `UserEntity` | **New.** Auth0 sub, email, name, telegram |
| `UserJobEntity` | **New.** Per-user job status + AI metadata |

### Enum: UserJobStatus

```kotlin
enum class UserJobStatus {
    NEW,        // matched but not reviewed
    APPLIED,    // user applied
    IRRELEVANT, // user marked as irrelevant
}
```

Replaces current `JobStatus` (removed from `JobEntity`).

---

## Job Matching Pipeline

### Flow

```
n8n scraper
    │
    ▼
POST /jobs/ingest (batch, atomic)
    │
    ▼
Save jobs to DB (dedup by URL)
Set matched_at = NULL for new jobs
    │
    ▼
@Scheduled (every N minutes)
Picks up: SELECT * FROM jobs WHERE matched_at IS NULL
    │
    ▼
For each unmatched job, for each user:
    │
    ├─ STAGE 1: Cold Filter (no AI, fast)
    │   ├─ Source allowed? (job.source not in user.disabled_sources)
    │   ├─ Remote match? (if user.remote_only → job.remote must be true)
    │   ├─ Excluded keywords? (job.title/description vs user.excluded_keywords)
    │   ├─ Category match? (job matches any user.categories)
    │   └─ Result: PASS / REJECT
    │
    ├─ STAGE 2: AI Filter (Claude Haiku, only if cold filter passed)
    │   ├─ Input: job title + description + user preferences
    │   ├─ Check: seniority match, tech stack relevance, overall fit
    │   ├─ Output: { relevant: boolean, score: 0-100, reasoning: string }
    │   └─ Result: MATCH (score >= threshold) / REJECT
    │
    ▼
If MATCH → create UserJobEntity(status=NEW, score, reasoning)
After all users processed → set job.matched_at = now()
```

### Why two stages?

| Stage | Cost | Speed | Purpose |
|-------|------|-------|---------|
| Cold filter | Free | <1ms/job | Eliminate obvious mismatches |
| AI (Haiku) | ~$0.001/call | ~500ms | Nuanced matching (seniority, relevance) |

With cold filter rejecting ~70-80% of jobs, AI costs stay minimal (~$1-3/month for 100 jobs/day).

### Failure handling

- If matching fails mid-batch → `matched_at` stays NULL → next @Scheduled picks it up
- If AI call fails → skip user for this job, log warning, retry on next cycle
- No data loss: unmatched jobs always get retried

---

## Preference Normalization (AI)

### Flow

```
User types free text in UI:
  "Looking for Senior/Lead Kotlin backend positions,
   remote only, $5000+ monthly, no crypto/web3"
         │
         ▼
POST /preferences/normalize
  → Claude Haiku normalizes to structured JSON
  → Returns preview to UI
         │
         ▼
User reviews & adjusts in UI
         │
         ▼
POST /preferences (or PUT /preferences/{id})
  → Saves structured preferences to DB
```

### Normalize endpoint

**Request:**
```json
{
  "rawInput": "Senior Kotlin backend, remote, $5000+, no crypto"
}
```

**Response (AI-generated, user reviews):**
```json
{
  "rawInput": "Senior Kotlin backend, remote, $5000+, no crypto",
  "categories": ["Java", "Kotlin"],
  "seniorityLevels": ["senior", "lead"],
  "keywords": ["kotlin", "backend", "spring", "java"],
  "excludedKeywords": ["crypto", "web3", "blockchain"],
  "minSalary": 5000,
  "remoteOnly": true,
  "disabledSources": []
}
```

User can edit any field before saving.

---

## API Endpoints

### Existing (fixed)

| Method | Path | Change |
|--------|------|--------|
| `POST` | `/jobs/ingest` | Wrap in single `@Transactional`, batch URL lookup, remove status from response |
| `GET` | `/criteria?source={SOURCE}` | No logic changes, verify enum name usage |

### New

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/preferences/normalize` | user token | AI-normalize raw text → structured preview |
| `GET` | `/preferences` | user token | Get current user's preferences |
| `PUT` | `/preferences` | user token | Save/update preferences (after normalization review) |
| `GET` | `/jobs` | user token | List jobs for current user (from UserJobEntity) |
| `PATCH` | `/jobs/{jobId}/status` | user token | Update job status for current user |

### Auth context

- n8n endpoints (`/jobs/ingest`, `/criteria`) — service-to-service scope: `write:jobs`, `read:criteria`
- User endpoints — Auth0 JWT with user identity, extract `sub` from token

---

## Database Migration V2

```sql
-- Remove status from jobs, add matched_at
ALTER TABLE jobs DROP COLUMN status;
ALTER TABLE jobs ADD COLUMN matched_at TIMESTAMPTZ;
CREATE INDEX idx_jobs_matched_at ON jobs (matched_at) WHERE matched_at IS NULL;

-- Fix description NOT NULL
ALTER TABLE jobs ALTER COLUMN description SET NOT NULL;

-- Drop old status index
DROP INDEX IF EXISTS idx_jobs_status;

-- Create users table
CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth0_sub        VARCHAR(255) UNIQUE NOT NULL,
    email            VARCHAR(255),
    name             VARCHAR(255),
    telegram_chat_id VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_auth0_sub ON users (auth0_sub);

-- Recreate user_preferences with user_id FK
-- (drop and recreate since no production data yet)
DROP TABLE user_preferences;

CREATE TABLE user_preferences (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID UNIQUE NOT NULL REFERENCES users(id),
    raw_input         TEXT,
    categories        TEXT[] NOT NULL DEFAULT '{}',
    seniority_levels  TEXT[] NOT NULL DEFAULT '{}',
    keywords          TEXT[] NOT NULL DEFAULT '{}',
    excluded_keywords TEXT[] NOT NULL DEFAULT '{}',
    min_salary        INTEGER,
    remote_only       BOOLEAN NOT NULL DEFAULT FALSE,
    disabled_sources  TEXT[] NOT NULL DEFAULT '{}',
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User-job relationship (per-user status tracking)
CREATE TABLE user_jobs (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id),
    job_id             UUID NOT NULL REFERENCES jobs(id),
    status             VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ai_relevance_score INTEGER,
    ai_reasoning       TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);

CREATE INDEX idx_user_jobs_user_status ON user_jobs (user_id, status);
CREATE INDEX idx_user_jobs_job_id ON user_jobs (job_id);
```

---

## Ingest Endpoint Fixes

### Before (current)
```
For each job in batch:
  TX1: findByUrl(url)
  TX2: save(entity)
→ N+1 queries, non-atomic, race conditions
```

### After
```
Single @Transactional on service method:
  1. Collect all URLs from batch
  2. Batch query: findByUrls(urls) → Map<url, entity>
  3. For each request:
     - existing? → update fields (title, description, salary, location, remote, publishedAt)
     - new? → create entity (matched_at = null)
  4. saveAll(entities)
→ 1 SELECT + 1 batch INSERT/UPDATE, fully atomic
```

### company not updated
Intentional. Company is part of job identity (same URL = same company). Only mutable data (salary, description) gets updated on re-scrape.

---

## AI Integration

### SDK & Auth

Uses [Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java) (`com.anthropic:anthropic-java`).

Two auth modes:
- **API Key** — standard `ANTHROPIC_API_KEY`
- **Auth Token** — OAuth token from `claude setup-token` (Max/Pro subscription, $0 extra)

Configuration: `jobhunter.ai.*` in `application.yml`, toggled via `AI_ENABLED` env var.

### Components

| Component | Role |
|-----------|------|
| `AiConfig` | Creates `AnthropicClient` bean (conditional on `ai.enabled=true`) |
| `AiProperties` | `@ConfigurationProperties` for all AI settings |
| `ClaudeClient` | Thin wrapper over SDK: sends message, extracts text response |
| `AiFilterService` | Builds prompt with job + preferences, parses JSON score/reasoning |
| `PreferenceNormalizeService` | Builds prompt with raw text, parses structured preferences |

### Graceful degradation

- `AI_ENABLED=false` → cold filter only, no API calls
- API call fails → job passes through on cold filter alone, error logged
- Response parsing fails → same as API failure, null result

---

## Package Structure (new/modified)

```
com.mshykhov.jobhunter/
├── config/
│   ├── AiConfig          # AnthropicClient bean
│   └── AiProperties      # AI configuration properties
├── controller/
│   ├── job/             # ingest (n8n) + user job list/status
│   ├── criteria/        # search criteria for n8n
│   └── preference/      # normalize + CRUD preferences
├── service/
│   ├── JobService        # ingest logic
│   ├── JobMatchingService # @Scheduled pipeline (cold + AI)
│   ├── ColdFilterService  # source, remote, keywords, categories
│   ├── AiFilterService    # Claude Haiku job relevance evaluation
│   ├── ClaudeClient       # Anthropic SDK wrapper
│   ├── PreferenceNormalizeService # AI preference normalization
│   ├── PreferenceService  # CRUD preferences
│   ├── UserJobService     # user job list/status
│   └── SearchCriteriaService
├── persistence/
│   ├── model/
│   │   ├── JobEntity      # (status removed, matched_at added)
│   │   ├── UserEntity     # Auth0 user
│   │   ├── UserJobEntity  # per-user job status + AI metadata
│   │   ├── UserPreferenceEntity # structured preferences
│   │   ├── JobSource
│   │   └── UserJobStatus  # NEW, APPLIED, IRRELEVANT
│   ├── repository/
│   └── facade/
└── exception/
```

---

## Implementation Order

| Step | What | Depends on |
|------|------|------------|
| 1 | V2 migration | — |
| 2 | Fix `JobEntity` (remove status, add matched_at) | Step 1 |
| 3 | Fix ingest endpoint (atomic, batch queries) | Step 2 |
| 4 | `UserEntity` + `UserJobEntity` + `UserPreferenceEntity` rework | Step 1 |
| 5 | Preference endpoints (normalize + CRUD) | Step 4 |
| 6 | `JobMatchingService` (cold filter) | Steps 3, 4 |
| 7 | `AiFilterService` (Claude Haiku) | Step 6 |
| 8 | User job list + status update endpoints | Step 4 |
| 9 | Telegram integration | Step 8 (later) |
