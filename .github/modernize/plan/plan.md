# Modernization Plan: Zava Loan Origination API on Azure

**Project**: ZavaLoanOriginationAPI

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Java Servlet API 4.0.1 (javax) on Tomcat 9
- **Build Tool**: Gradle
- **Database**: Microsoft SQL Server (JDBC)
- **Key Dependencies**: javax.servlet-api, mssql-jdbc, org.json

---

## Overview

> This migration modernizes the Zava Loan Origination API to the latest Java
> framework baseline and deploys it to Azure using cloud-native services.
> The application currently runs as a Java 11 servlet WAR with environment-
> based configuration and SQL Server connectivity. The new architecture will:
>
> - move the runtime/framework baseline to a current supported Java stack
> - replace sensitive configuration and connectivity with Azure-native services
> - package and deploy the service to Azure Container Apps for cloud operation
>
> The migration follows a phased approach: upgrade first, transform to Azure
> managed services, harden security posture, and then deploy.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| ZavaLoanOriginationAPI | Java 11 + Tomcat 9 | Latest Java framework baseline | N/A | Modernize runtime/framework |
| ZavaLoanOriginationAPI | App secrets in env/config | Azure Key Vault | Managed Identity | Move secrets to cloud-native store |
| ZavaLoanOriginationAPI | SQL Server auth credentials | Azure SQL + MI auth | Managed Identity | Use cloud-native DB auth model |
| ZavaLoanOriginationAPI | Local/container runtime | Azure Container Apps | Managed Identity | Deploy modernized app to Azure |

---

## Security Compliance

**Description**: Scan all project dependencies for known CVEs and remediate any
identified vulnerabilities to ensure the application is secure before deployment.

**Requirements**:
- Upgrade vulnerable dependencies to minimum patched versions.
- If a fix requires a major upgrade, document dependency/version/risk impacts.
- Verify the project build and tests after remediation.
