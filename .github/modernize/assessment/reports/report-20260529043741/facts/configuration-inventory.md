# Configuration & Externalized Settings Inventory

ZavaLoanOriginationAPI uses a single flat properties file (`loan-origination.properties`) combined with Dockerfile-baked environment variable defaults as its two configuration sources; there are no runtime profiles, no secrets management system, and no feature flags.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| `loan-origination.properties` | Java properties file | `src/main/resources/loan-origination.properties` | Classpath resource; provides default values for all database and service endpoints. Read by `LoanOriginationConfig` at class-load time via `ClassLoader.getResourceAsStream`. |
| `web.xml` | Servlet deployment descriptor | `src/main/webapp/WEB-INF/web.xml` | Declares all four Servlets, their URL mappings, and `load-on-startup` order. Java EE 4.0 descriptor format. |
| `Dockerfile` ENV directives | Container environment variables | `Dockerfile` (lines 7–14) | Bakes default values for all configuration keys directly into the container image layer. These are overridable at container runtime via `-e` flags or orchestrator env injection. |

No Spring Cloud Config server, Azure App Configuration, AWS AppConfig, HashiCorp Vault, or any other external configuration server is in use.

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| Default (single) | Always active (no profiles defined) | Compiles Java 11 sources and packages a WAR named `ROOT.war` | `java` plugin, `war` plugin; `javax.servlet-api:4.0.1` (compileOnly), `mssql-jdbc:12.6.3.jre11`, `org.json:20140107` |

There are no Maven/Gradle build profiles, conditional compilation symbols, or environment-specific build variants. The `build.gradle` defines a single build configuration.

## Runtime Profiles

No runtime profiles are defined. The application uses neither Spring Boot profile mechanisms (`application-{profile}.properties`) nor any other profile-switching facility. Configuration is determined entirely by environment variable presence at startup; the properties file provides the fallback.

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| (default — single profile) | Always active; no profile selection | `loan-origination.properties` | All values overridable via corresponding environment variables (see Properties Inventory) |

## Properties Inventory

### ZavaLoanOriginationAPI

| Property Key | Env Variable Override | Default Value | Source |
|---|---|---|---|
| `db.host` | `DB_HOST` | `sqlserver` | `loan-origination.properties` / Dockerfile ENV |
| `db.port` | `DB_PORT` | `1433` | `loan-origination.properties` / Dockerfile ENV |
| `db.name` | `DB_NAME` | `ZavaBankDB` | `loan-origination.properties` / Dockerfile ENV |
| `db.user` | `DB_USER` | `sa` | `loan-origination.properties` / Dockerfile ENV |
| `db.password` | `DB_PASSWORD` | `YourStrong!Passw0rd` | `loan-origination.properties` / Dockerfile ENV — **hardcoded secret** |
| `kyc.service.baseUrl` | `KYC_SERVICE_BASE_URL` | `http://zava-kyc-service:8080` | `loan-origination.properties` / Dockerfile ENV |
| `risk.service.baseUrl` | `RISK_SERVICE_BASE_URL` | `http://zava-risk-engine:8080` | `loan-origination.properties` / Dockerfile ENV |
| `ledger.service.baseUrl` | `LEDGER_SERVICE_BASE_URL` | `http://zava-ledger:8080` | `loan-origination.properties` / Dockerfile ENV |
| `loan.default.interestRate` | `LOAN_DEFAULT_INTEREST_RATE` | `0.0725` (7.25%) | `loan-origination.properties` |

Resolution order: `LoanOriginationConfig.read()` checks `System.getenv(envKey)` first; if blank or absent, falls back to `PROPERTIES.getProperty(propertyKey)`. There is no system-property (`-D`) tier or config server tier.

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | CPU | Instance Count |
|---|---|---|---|---|
| ZavaLoanOriginationAPI (Tomcat) | None explicitly configured; Tomcat default JVM settings apply | Not specified — no Docker `mem_limit`, no Kubernetes `resources` defined | Not specified | 1 (no scaling configuration) |

No `-Xms`/`-Xmx` heap settings, no GC tuning flags, and no JVM system properties are defined anywhere in the project. The Dockerfile uses Tomcat's default `catalina.sh run` startup command with no custom JVM arguments.

## Startup Dependency Chain

The startup sequence within the single container is:

1. **Tomcat container starts** → loads the `ROOT.war` web application
2. **`LoanBootstrapServlet.init()`** (declared `load-on-startup=1` in `web.xml`) → connects to SQL Server → executes `CREATE TABLE IF NOT EXISTS` DDL for `LoanApplications` and `LoanDecisions`
3. **All other Servlets become available** → `LoanApplyServlet` and `LoanStatusServlet` begin accepting requests at `/api/loans/*`

There is no readiness probe, no health-check wait loop (`dockerize`, `wait-for-it.sh`, Kubernetes `readinessProbe`), and no Docker Compose `depends_on` ordering in the repository. If SQL Server is unavailable at startup, `LoanBootstrapServlet.init()` throws a `ServletException` that causes Tomcat to fail the application deployment.

External service availability (KYC, Risk Engine, Ledger) is not checked at startup; failures are silently swallowed at runtime by `callJsonApi`.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage | Notes |
|---|---|---|---|
| `db.password` / `DB_PASSWORD` | Database password | Plaintext in `loan-origination.properties` and Dockerfile `ENV` — **[HARDCODED]** | Default value `YourStrong!Passw0rd` is committed to source control in both the properties file and the Docker image layer |
| `db.user` / `DB_USER` | Database username | Plaintext in `loan-origination.properties` and Dockerfile `ENV` — **[HARDCODED]** | Uses SQL Server `sa` (system administrator) account |

### Secrets Provisioning Workflow

There is no secrets management system. Both the database password and username are committed to source control in plaintext (`loan-origination.properties`) and baked into the Docker image via `ENV` directives (`Dockerfile`). The `DB_PASSWORD` environment variable can be overridden at container runtime via orchestrator injection (e.g., Kubernetes `secretKeyRef`, Docker Compose `env_file`), but no such configuration exists in this repository.

The default `sa` account with a hardcoded password represents a critical security risk. No Key Vault, Vault, AWS Secrets Manager, Jasypt encryption, or sealed secrets mechanism is in use.

## Feature Flags

No feature flags, conditional beans, A/B testing toggles, or feature management frameworks are used. The application has no `@ConditionalOnProperty`, LaunchDarkly, Unleash, or any other feature toggle mechanism.

| Flag Name | Default | Controlled By |
|---|---|---|
| (none detected) | — | — |

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java (source/target compatibility) | 11 | `build.gradle` (`sourceCompatibility = JavaVersion.VERSION_11`) |
| Gradle (build tool) | 7.6 | `Dockerfile` (`FROM gradle:7.6-jdk11 AS build`) |
| Apache Tomcat (servlet container) | 9 | `Dockerfile` (`FROM tomcat:9-jdk11`) |
| Jakarta EE Servlet API | 4.0.1 | `build.gradle` (`javax.servlet:javax.servlet-api:4.0.1`) |
| Microsoft JDBC Driver for SQL Server | 12.6.3.jre11 | `build.gradle` (`com.microsoft.sqlserver:mssql-jdbc:12.6.3.jre11`) |
| org.json | 20140107 | `build.gradle` (`org.json:json:20140107`) |
| Docker base image (build) | `gradle:7.6-jdk11` | `Dockerfile` |
| Docker base image (runtime) | `tomcat:9-jdk11` | `Dockerfile` |
