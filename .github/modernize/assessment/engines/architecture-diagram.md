# Architecture Diagram

ZavaLoanOriginationAPI is a Jakarta EE (Servlet-based) Java 11 application deployed as a WAR on Apache Tomcat 9, providing a loan origination REST API that integrates with KYC, Risk Engine, and Ledger microservices while persisting data to Microsoft SQL Server.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Consumer["API Consumer (HTTP Client)"]
    end

    subgraph App["Application Layer - Jakarta EE / Tomcat 9 (Java 11)"]
        Health["HealthServlet\n/health"]
        Bootstrap["LoanBootstrapServlet\n/internal/bootstrap"]
        LoanApply["LoanApplyServlet\n/api/loans/apply"]
        LoanStatus["LoanStatusServlet\n/api/loans/{id}/status"]
        Config["LoanOriginationConfig\n(env + properties)"]
        ConnFactory["LoanConnectionFactory\n(JDBC / SQL Server Driver)"]
    end

    subgraph Data["Data Layer"]
        MSSQL[("SQL Server\nZavaBankDB")]
    end

    subgraph External["External Microservices"]
        KYC["KYC Service\nzava-kyc-service:8080"]
        Risk["Risk Engine\nzava-risk-engine:8080"]
        Ledger["Ledger Service\nzava-ledger:8080"]
    end

    Consumer -->|"GET /health"| Health
    Consumer -->|"POST /api/loans/apply"| LoanApply
    Consumer -->|"GET /api/loans/{id}/status"| LoanStatus
    Bootstrap -->|"CREATE TABLE on startup"| ConnFactory
    LoanApply -->|"INSERT / UPDATE"| ConnFactory
    LoanStatus -->|"SELECT"| ConnFactory
    ConnFactory -->|"JDBC SQL queries"| MSSQL
    LoanApply -->|"POST /api/kyc/verify"| KYC
    LoanApply -->|"POST /api/risk/score"| Risk
    LoanApply -->|"POST /api/accounts/create"| Ledger
    Config -.->|"provides DB and service URLs"| ConnFactory
    Config -.->|"provides service URLs"| LoanApply
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Runtime | Apache Tomcat | 9 | Java Servlet container hosting the WAR |
| Language | Java | 11 | Application source language |
| Build Tool | Gradle | 7.6 | Build and WAR packaging |
| Web Framework | Jakarta EE Servlet API | 4.0 | HTTP request/response handling |
| Database Driver | Microsoft JDBC Driver for SQL Server | 12.6.3 | JDBC connectivity to SQL Server |
| JSON Processing | org.json | 20140107 | JSON parsing and response serialization |
| Containerization | Docker (multi-stage build) | — | Build (gradle:7.6-jdk11) → Runtime (tomcat:9-jdk11) |

### Data Storage & External Services

The application stores all loan application records and decisions in **Microsoft SQL Server** (database `ZavaBankDB`) via raw JDBC with the `com.microsoft.sqlserver:mssql-jdbc` driver. Two tables are auto-created at startup by `LoanBootstrapServlet`: `LoanApplications` (loan lifecycle data) and `LoanDecisions` (credit decision audit trail). The application orchestrates three downstream HTTP microservices for each loan decision: a **KYC Service** (`zava-kyc-service:8080`) for identity verification, a **Risk Engine** (`zava-risk-engine:8080`) for credit scoring, and a **Ledger Service** (`zava-ledger:8080`) for loan account creation. Service URLs and database credentials are sourced from environment variables or from `loan-origination.properties`.

### Key Architectural Decisions

- **Raw JDBC without ORM**: The application uses `DriverManager.getConnection` with `PreparedStatement` directly rather than JPA or any ORM, keeping the data layer lightweight but coupling SQL tightly to the servlet code.
- **Synchronous orchestration pipeline**: Loan decisioning calls KYC → Risk Engine → Ledger sequentially within a single HTTP request/response cycle, using a single JDBC transaction for SQL writes.
- **Configuration via environment variables with properties file fallback**: `LoanOriginationConfig` reads environment variables first, falling back to `loan-origination.properties` for all database and service endpoint configuration.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation["Presentation (Servlets)"]
        HealthSrv["HealthServlet"]
        BootstrapSrv["LoanBootstrapServlet"]
        ApplySrv["LoanApplyServlet"]
        StatusSrv["LoanStatusServlet"]
    end

    subgraph Business["Business Logic"]
        Orchestration["Orchestration\n(inside LoanApplyServlet)"]
    end

    subgraph DataAccess["Data Access"]
        ConnFactory["LoanConnectionFactory"]
    end

    subgraph Infrastructure["Infrastructure"]
        Config["LoanOriginationConfig"]
    end

    BootstrapSrv -->|"init DB schema"| ConnFactory
    ApplySrv -->|"delegates decision"| Orchestration
    ApplySrv -->|"INSERT / UPDATE"| ConnFactory
    StatusSrv -->|"SELECT"| ConnFactory
    Orchestration -->|"calls KYC, Risk, Ledger"| Orchestration
    ConnFactory -->|"reads DB config"| Config
    ApplySrv -->|"reads service URLs"| Config
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| HealthServlet | Presentation | Jakarta EE HttpServlet | Returns a plain-text health status page at `/health` |
| LoanBootstrapServlet | Presentation | Jakarta EE HttpServlet | Auto-creates `LoanApplications` and `LoanDecisions` tables in SQL Server on application startup |
| LoanApplyServlet | Presentation / Business Logic | Jakarta EE HttpServlet | Accepts POST `/api/loans/apply`, orchestrates KYC → Risk → Ledger calls, persists application record and decision |
| LoanStatusServlet | Presentation | Jakarta EE HttpServlet | Accepts GET `/api/loans/{id}/status`, queries SQL Server and returns loan application state as JSON |
| LoanOriginationConfig | Infrastructure | Configuration utility (static) | Resolves all configuration values (DB URL/credentials, service base URLs, default interest rate) from env vars with properties file fallback |
| LoanConnectionFactory | Data Access | JDBC factory (static) | Loads SQL Server JDBC driver and creates raw `java.sql.Connection` objects using credentials from `LoanOriginationConfig` |
