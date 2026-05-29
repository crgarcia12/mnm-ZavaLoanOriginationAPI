# Configuration & Externalized Settings Inventory

This project has a compact configuration surface made up of one bundled properties file, environment-variable overrides, container defaults, and servlet deployment descriptors. Secrets handling is basic and relies on static configuration rather than an external secret store.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| Loan origination properties | Properties file | `src/main/resources/loan-origination.properties` | Default database, downstream service, and business settings |
| Process environment | Environment variables | Runtime environment | Overrides database and downstream service settings via `System.getenv` |
| Container image defaults | Dockerfile `ENV` directives | `Dockerfile` | Provides default values for DB and downstream URLs in the image |
| Servlet deployment descriptor | XML | `src/main/webapp/WEB-INF/web.xml` | Declares servlet classes, mappings, and startup servlet |
| Build configuration | Gradle | `build.gradle` | Declares dependencies and WAR packaging |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| Default build | Automatic | Compiles Java sources and packages a WAR | `java` plugin, `war` plugin |

No additional Maven or Gradle profiles were detected.

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| Default runtime | Automatic | `loan-origination.properties` | Database host, port, name, user, password, service base URLs, default interest rate |
| Container runtime | Environment variables | Dockerfile `ENV` values | Overrides `DB_*`, `KYC_SERVICE_BASE_URL`, `RISK_SERVICE_BASE_URL`, `LEDGER_SERVICE_BASE_URL` |

No named Spring or Jakarta runtime profiles were detected.

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| `db.host` | `sqlserver` | Default, container override | Properties file or `DB_HOST` |
| `db.port` | `1433` | Default, container override | Properties file or `DB_PORT` |
| `db.name` | `ZavaBankDB` | Default, container override | Properties file or `DB_NAME` |
| `db.user` | `sa` | Default, container override | Properties file or `DB_USER` |
| `db.password` | `[MASKED]` | Default, container override | Properties file or `DB_PASSWORD` |
| `kyc.service.baseUrl` | `http://zava-kyc-service:8080` | Default, container override | Properties file or `KYC_SERVICE_BASE_URL` |
| `risk.service.baseUrl` | `http://zava-risk-engine:8080` | Default, container override | Properties file or `RISK_SERVICE_BASE_URL` |
| `ledger.service.baseUrl` | `http://zava-ledger:8080` | Default, container override | Properties file or `LEDGER_SERVICE_BASE_URL` |
| `loan.default.interestRate` | `0.0725` | Default | Properties file or `LOAN_DEFAULT_INTEREST_RATE` |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| Zava Loan Origination API | No JVM options or system properties declared in repository | Not specified | Not specified |

## Startup Dependency Chain

1. SQL Server must be reachable before the web application finishes startup because `LoanBootstrapServlet` creates the schema in `init()`.
2. Tomcat hosts the WAR and exposes servlet mappings once initialization succeeds.
3. KYC, risk, and ledger services must be reachable for the full loan-application workflow; otherwise the submission path falls back to incomplete or degraded responses.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| `db.password` / `DB_PASSWORD` | Database password | Bundled properties file and Dockerfile `ENV` value `[MASKED]` |
| JDBC connection string | Database connection | Constructed in code; disables encryption and trusts the server certificate |

### Secrets Provisioning Workflow

Secrets are provisioned through static application configuration rather than a managed secret store. The application first loads `loan-origination.properties`, then allows environment variables to override those values at runtime. No Key Vault, Vault, encrypted properties, or deployment-time secret injection workflow was detected, so the web application, database access, and downstream service integrations all depend on values that are either bundled in source or injected as plain environment variables.

## Feature Flags

| Flag Name | Default | Controlled By |
|---|---|---|
| None detected | N/A | N/A |

No conditional feature-flag framework or property-based feature toggles were detected.

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java target | 11 | `build.gradle` |
| Servlet API | 4.0.1 | `build.gradle` |
| SQL Server JDBC driver | 12.6.3.jre11 | `build.gradle` |
| JSON library | 20140107 | `build.gradle` |
| Build image | Gradle 7.6 on JDK 11 | `Dockerfile` |
| Runtime image | Tomcat 9 on JDK 11 | `Dockerfile` |
