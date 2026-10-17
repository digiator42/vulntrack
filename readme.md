# VulnTrack - Enterprise Vulnerability Management System

<div align="center"> <img src="https://img.shields.io/badge/Spring_Boot-3.2.1-brightgreen" alt="Spring Boot"/> <img src="https://img.shields.io/badge/Next.js-14.0-blue" alt="Next.js"/> <img src="https://img.shields.io/badge/PostgreSQL-16-336791" alt="PostgreSQL"/> <img src="https://img.shields.io/badge/Java-21-007396" alt="Java"/> <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/> </div>

> From manual data entry to automated, enriched, and collaborative security management.

**Highlights**
- 🔀 [Hybrid OSV + NVD matching](#-hybrid-osv--nvd-matching-showcase) — two independent sources, merged on canonical CVE id
- 🔄 Resumable [NVD cache sync](#-cve-cache-sync-showcase) with a database lease and stall watchdog
- 🦀 Rust (`Cargo.toml` / `Cargo.lock`), Maven, NPM, PyPI manifest scanning
- 📄 Role-scoped PDF posture reports, live issue chat, CVSS v3.1 calculator

---

## The Problem

Traditional vulnerability management tools require security teams to manually:

- Look up CVE details from NVD
- Enter CVSS scores manually
- Match CPE names correctly (often leading to misidentification)
- Track vulnerabilities across dependencies
- Collaborate across teams without context
---

## Eliminating CPE Naming Chaos

### The CPE Problem

One of the biggest challenges in vulnerability management is **CPE (Common Platform Enumeration) naming**. Different vendors use different naming conventions:

|User Types|Actual CPE|
|---|---|
|`spring`|`cpe:2.3:a:vmware:spring_framework`|
|`spring-boot`|`cpe:2.3:a:vmware:spring_boot`|
|`log4j`|`cpe:2.3:a:apache:log4j`|
|`snakeyaml`|`cpe:2.3:a:snakeyaml_project:snakeyaml`|

### VulunTrack Solution

We integrated **[TII's CPEdict](https://github.com/tiiuae/cpedict)** - a curated CPE dictionary with over **64,000 mappings**. This solves the CPE naming ambiguity problem:
```mermaid
graph LR
    A["User Searches #quot;spring#quot;"] --> B{"Exact Match?"}
    B -->|"No"| C["Fuzzy Suggestions"]
    C --> D["User Selects Correct Product"]
    D --> E["Auto-Enrich from NVD"]
    B -->|"Yes"| E
    E --> F["Show CVEs for that CPE"]
```
---
## 🏗️ Architecture Overview

```text

┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (Next.js)                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐     │
│  │   CVSS v3.1  │ │   CVE/CWE    │ │  Dependency Scanner  │     │
│  │  Calculator  │ │   Lookup     │ │  (pom.xml, package)  │     │
│  └──────────────┘ └──────────────┘ └──────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Backend (Spring Boot)                    │
│  ┌─────────────┐ ┌─────────────────┐ ┌────────────────────┐     │ 
│  │   RBAC      │ │  Local NVD DB   │ │  CPE Mapping Engine│     │
│  │ (3 Roles)   │ │  (300k+ CVEs)   │ │  (64k+ CPEs)       │     │
│  └─────────────┘ └─────────────────┘ └────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PostgreSQL Database                        │
│  ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌─────────────┐     │
│  │   Issues   │ │  Comments  │ │  Users   │ │  CVE Cache  │     │
│  │   (UUID)   │ │ (Activity) │ │  (RBAC)  │ │ (800k rows) │     │
│  └────────────┘ └────────────┘ └──────────┘ └─────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```
---

## 🔀 Hybrid OSV + NVD Matching (showcase)

A dependency scan doesn't ask one database — it asks **two independent ones**
and merges the answers, because each covers the other's blind spot:

| | [OSV](https://osv.dev/) | Local NVD CPE cache |
| --- | --- | --- |
| Keyed by | ecosystem + package + version (exact) | CPE vendor/product + version ranges |
| Weakness | covers published package ecosystems only | needs CPE guessing (`spring` → `vmware:spring_framework`) |
| Strength | no guessing, fresh advisories, CVSS-v4 + CWE | broad: OS packages, network gear, non-ecosystem software |

Rather than forcing a user to pick (a checkbox) or dumping both result sets
(a duplicated mess), findings are **merged on the canonical CVE id**. This
matters because OSV reports advisory ids (`GHSA-*`, `PYSEC-*`) that *alias* a
CVE, so the alias is what makes deduplication possible at all.

Every finding carries its provenance, surfaced in the UI:

| Badge | Meaning |
| --- | --- |
| `✓ OSV + NVD` | Two independent sources agree — highest confidence |
| `OSV` | Ecosystem-native advisory database only |
| `NVD` | Local CPE cache only |

Measured on a real PyPI manifest, the hybrid pass returned **10 OSV hits
against 5 NVD hits** for the same three dependencies — exactly the blind spot
the CPE path has.

```mermaid
graph LR
    A["Manifest upload"] --> B["OSV querybatch<br/>(ecosystem-native)"]
    A --> C["NVD CPE cache<br/>(local, broad)"]
    B --> D["Resolve aliases<br/>GHSA-* to CVE-*"]
    D --> E{"Merge on<br/>canonical CVE id"}
    C --> E
    E --> F["Findings tagged<br/>OSV / NVD / both"]
```

Implementation notes:
- **Batched, not N calls.** One `POST /v1/querybatch` per manifest (up to 1000
  packages) instead of a request per dependency.
- **Cached.** Advisory detail is stored in `osv_advisory` (24h TTL) so repeat
  scans cost one table read; `dependency_vulnerability_cache` is pre-provisioned
  for per-(package, version) short-circuiting.
- **Degrades gracefully.** An OSV outage falls back to NVD-only rather than
  failing the scan — and vice versa.

---

## 📦 Local NVD Database

Instead of making **live API calls** to NVD (slow, rate-limited, unreliable):

- ✅ **300,000+ CVEs / 800k rows** pre-populated locally (A vendor could have multiple CVES)
    
- ✅ **Raw JSON Storage** - Store the complete NVD JSON for each CVE
    

### 🔄 CVE Cache Sync (showcase)

Dependency scans read from the local `local_cve` cache — never from the live
NVD API — so the cache is kept fresh on its own schedule (`NvdSyncService`):

| Job | Schedule | Scope |
| --- | --- | --- |
| Incremental sync | Daily 02:00 | CVEs modified since the last run |
| Full sync | Sundays 02:00 | Entire NVD catalog |

Need data *now* (e.g. right after a fresh deploy, when the cache is still
empty)? Admins can trigger a full sync on demand. A full sync pages through
~250k CVEs and outlasts any HTTP timeout, so it runs on a background thread
and returns `202 Accepted` immediately — poll the status endpoint until
`running` is `false`:

```shell
# Start a full sync (admin JWT required)
curl -X POST https://<backend>/api/admin/nvd/sync \
  -H "Authorization: Bearer $TOKEN"
# {"status":"started", ...} | {"status":"already_running", ...}

# Poll progress
curl https://<backend>/api/admin/nvd/sync/status \
  -H "Authorization: Bearer $TOKEN"
# {"running":true}
```

Overlapping runs (scheduled vs. manual) are rejected, so the cache can never
be double-filled. A unique identity index on
`(cve_id, vendor, product, version bounds)` also guarantees no duplicate
rows — same product with *different* version ranges stays as separate rows
on purpose, since version matching needs them.

The sync is resumable: every fetched page persists its position to the
`nvd_sync_state` table, so an interrupted run (deploy, recycle) continues
where it stopped instead of starting over — a reboot even auto-resumes it.
`GET /api/admin/nvd/sync/status` reports the live position
(`lastStartIndex` / `totalResults`), and the scanner page shows it as a
progress badge. To jump to an explicit page instead of the saved position:

```shell
curl -X POST "https://<backend>/api/admin/nvd/sync?from=23000" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Features

### 1. **Smart CVE/CWE Lookup**

- Auto-populates CVE details when user types a CVE ID
- Fetches CVSS score, vector, description automatically
- Suggests CWE based on CVE data

### 2. **CVSS v3.1 Calculator**

- Interactive calculator with real-time score updates
- Live vector string generation
- Auto-populates severity based on CVSS score

### 3. **Dependency Scanner**

- Upload `pom.xml` (Maven), `package.json` (NPM), `requirements.txt` (Python), `Cargo.toml` / `Cargo.lock` (Rust)
- Auto-detects vulnerable dependencies against **two independent sources** (see [Hybrid OSV + NVD matching](#-hybrid-osv--nvd-matching-showcase))
- Creates issues for each CVE automatically
- Supports both public (no auth) and authenticated scans
- Prefer `Cargo.lock` when available — it carries resolved versions, while `Cargo.toml` only declares semver requirements

### 4. **Role-Based Access Control (RBAC)**

| Role                    | Permissions                                                   |
| ----------------------- | ------------------------------------------------------------- |
| **ROLE_ADMIN**          | Full access: manage users, delete any issue, view all reports |
| **ROLE_SECURITY_ADMIN** | Create/edit issues, CVE lookups, view global metrics          |
| **ROLE_DEVELOPER**      | View only assigned issues, update status (OPEN → RESOLVED)    |

### 5. **Collaboration & Live Chat**
 - Each issue page has an integrating live chat, that also works as activity tracking all over the issue lifecycle.

- Real-time comments via WebSocket
- Activity feed showing all status changes
- Auto-logs: "Researcher changed status to IN_PROGRESS"

### 6. **Professional Reports**

- Role-based PDF reports (admins see global, developers see assigned)
- Severity breakdown charts
- CWE distribution
- CVSS average scores

### 7. **Public Scanner**

- **Free public tool** for anyone to check dependencies
- No registration required
- CPE lookup tool with suggestions
- View CVEs for any CPE

---
## 🔗 API Endpoints

### Reach `http://localhost:8080/swagger-ui/index.html`

## Run
- A Backend dev profile integrated in repo, just export env in linux or windows and run it with:

```shell
$env:SPRING_PROFILES_ACTIVE="dev"; ./mvnw spring-boot:run
```

- Frontend
```shell
bun run dev
```

## Login

```
Admin creds -> admin : password
Dev1 creds  -> dev1 : password
Dev2 creds  -> dev2 : password
```

## PG database
- You can find PG database backup in root, add it to your current databases, name it with vuluntrack_dev, so spring boot can connect to it.

```
psql -U username -d database_name -f path/to/file.sql
```
