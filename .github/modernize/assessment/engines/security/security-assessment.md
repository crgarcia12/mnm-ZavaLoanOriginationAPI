# Security Assessment Report

**Generated:** 2026-05-28T23:23:35Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 8 |
| CVE Vulnerabilities | 2 |
| CWE Vulnerabilities | 6 |
| Total Rules Assessed | 59 |
| Rules Passed | 53 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 2 |
| optional | 3 |
| potential | 3 |

## CVE Findings (Dependency Vulnerabilities)

### CVE-2023-5072: Java: DoS Vulnerability in JSON-JAVA
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2023-5072](https://github.com/advisories/GHSA-4jq9-2xhw-jpx7): Java: DoS Vulnerability in JSON-JAVA

Severity: HIGH

A denial of service vulnerability in JSON-Java. A bug in the parser means that an input string of modest size can lead to indefinite amounts of memory being used, potentially causing OutOfMemoryError.

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Vulnerable version range: <= 20230618

Recommended fix:
  - Upgrade org.json:json to 20231013 or later

---

### CVE-2022-45688: json stack overflow vulnerability
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2022-45688](https://github.com/advisories/GHSA-3vqj-43w4-2q58): json stack overflow vulnerability

Severity: HIGH

A stack overflow in the XML.toJSONObject component of org.json:json before version 20230227 allows attackers to cause a Denial of Service (DoS) via crafted JSON or XML data.

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Vulnerable version range: < 20230227

Recommended fix:
  - Upgrade org.json:json to 20230227 or later

---

## CWE Findings (Code-Level Vulnerabilities)

### CWE-477: Use of Obsolete Function
- **Category:** Code Quality
- **Severity:** optional
- **Story Points:** 1
- **Files:** src/main/java/com/zavabank/loanorigination/LoanConnectionFactory.java

In LoanConnectionFactory (line 10), `Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")` is used to explicitly load the JDBC driver. This pattern has been obsolete since JDBC 4.0 (Java SE 6), which introduced automatic driver loading via `java.util.ServiceLoader`. The explicit `Class.forName()` call is no longer required and indicates unmaintained code.

---

### CWE-681: Incorrect Conversion between Numeric Types
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

In `LoanApplyServlet.doPost()` (line 37), a `double` value obtained via `payload.optDouble("requestedAmount", 0d)` is directly converted to `BigDecimal` using `new BigDecimal(double)`. IEEE 754 floating-point representation can cause imprecision (e.g., 123.45 may become 123.4499999999...). For financial loan amounts, this conversion may produce incorrect monetary values in database records and downstream decisions.

---

### CWE-789: Memory Allocation with Excessive Size Value
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 5
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

In `LoanApplyServlet.readBody()` (lines 296-305), the method reads the entire HTTP request body into a `StringBuilder` without enforcing any maximum size limit. An attacker can send an arbitrarily large request body, causing unbounded memory allocation and potential heap exhaustion (denial of service). There is no check on `Content-Length` or total bytes read before accumulating all lines into memory.

---

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/loan-origination.properties

In `loan-origination.properties` (line 5), the database password is hard-coded. This properties file is bundled into the WAR artifact and deployed to the server, exposing the database password to anyone with access to the deployment package or the application classpath.

---

### CWE-778: Insufficient Logging
- **Category:** Credentials & Secrets
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java, src/main/java/com/zavabank/loanorigination/LoanStatusServlet.java

The application has no logging framework (no SLF4J, Log4j, or `java.util.logging` imports) across any servlet. Security-critical events such as loan application submissions, KYC verification results, risk scoring decisions, loan approvals/declines, and database errors are handled silently (e.g., `catch (SQLException exception)` blocks in `LoanApplyServlet` return a generic error response with no log record). This makes security incident detection and audit trail impossible.

---

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/loan-origination.properties

In `loan-origination.properties` (lines 4-5), both the database username (`db.user=sa`) and password are hard-coded. The `sa` account is the SQL Server built-in system administrator, and using it with a hard-coded password in a properties file bundled into the deployment artifact represents a critical credential exposure risk.
