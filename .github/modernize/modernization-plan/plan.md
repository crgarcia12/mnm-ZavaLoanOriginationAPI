# Modernization Plan: modernization-plan

**Project**: ZavaLoanOriginationAPI

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Java EE / Jakarta EE (Servlet-based web application)
- **Build Tool**: Gradle
- **Database**: Microsoft SQL Server (JDBC)
- **Key Dependencies**: javax.servlet-api, mssql-jdbc, org.json

---

## Overview

> This migration modernizes the Java loan origination application for Azure.
> The application currently runs as a servlet-based workload with direct
> SQL authentication and known dependency CVE risks. The new architecture
> will:
>
> - Improve security by removing plaintext credentials from the app config
> - Move database access toward Azure-native managed identity patterns
> - Package and deploy the app to Azure with a cloud deployment workflow
>
> The migration follows a phased approach covering security-sensitive
> code transformations, CVE remediation, and Azure deployment readiness.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| ZavaLoanOriginationAPI | SQL auth config | Azure Key Vault + Azure SQL | Managed Identity | Remove plaintext secrets and modernize DB auth |
| ZavaLoanOriginationAPI | Current runtime host | Azure Container Apps | Managed Identity | Deploy modernized Java app on Azure |

---

## Security Compliance

**Description**: Scan all project dependencies for known CVEs and remediate any identified vulnerabilities to ensure the application is secure before deployment.

**Requirements**:
Upgrade vulnerable dependencies to the minimum patched version. If a CVE fix requires a major version upgrade, document the affected dependency, the current version, the upgraded major version, and the breaking change risk. Verify that the project builds and all tests pass after remediation.

**Environment Configuration**:
Runtime and build tooling established by current project setup.

**App Scope**:
- `.`

**Skills**:
- Skill Name: validate-cves-and-fix
  - Skill Location: builtin
