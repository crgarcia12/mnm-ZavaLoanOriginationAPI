# Security Assessment Report

**Generated:** 2026-05-29T04:37:41.0000000Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 13 |
| CVE Vulnerabilities | 2 |
| CWE Vulnerabilities | 11 |
| Total Rules Assessed | 59 |
| Rules Passed | 48 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 2 |
| optional | 4 |
| potential | 7 |

### By Category

| Category | Count |
|----------|-------|
| CVE | 2 |
| Code Quality | 7 |
| Credentials & Secrets | 4 |

---

## CVE Findings (Dependency Vulnerabilities)

### CVE-2023-5072: Java: DoS Vulnerability in JSON-JAVA
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2023-5072](https://github.com/advisories/GHSA-4jq9-2xhw-jpx7): Java: DoS Vulnerability in JSON-JAVA

Severity: HIGH

A denial of service vulnerability in JSON-Java (org.json). A bug in the parser means that an input string of modest size can lead to indefinite amounts of memory being used. An attacker can exploit nested JSON objects with deeply recursive key structures to trigger an OutOfMemoryError.

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17) — vulnerable_version_range: <= 20230618

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
  - org.json:json:20140107 (declared at build.gradle:17) — vulnerable_version_range: < 20230227

Recommended fix:
  - Upgrade org.json:json to 20231013 or later (addresses both CVE-2022-45688 and CVE-2023-5072)

---

## CWE Findings (Code-Level Vulnerabilities)

### CWE-477: Use of Obsolete Function
- **Category:** Code Quality
- **Severity:** optional
- **Story Points:** 1
- **Files:** src/main/java/com/zavabank/loanorigination/LoanConnectionFactory.java

`Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")` is called explicitly in the static initializer of `LoanConnectionFactory` (line 10). JDBC 4.0+ (Java 6+) supports automatic driver discovery via the ServiceLoader mechanism; explicit `Class.forName` driver loading has been deprecated practice since Java SE 6 / JDBC 4.0.

---

### CWE-606: Unchecked Input for Loop Condition
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

In `readBody()` (line 299), a `while ((bytesRead = inputStream.read(buffer)) != -1)` loop reads user-supplied HTTP body bytes into a `StringBuilder` without imposing any maximum size limit on the accumulated data. An attacker can send an arbitrarily large request body, causing the loop to continue indefinitely or until the JVM runs out of heap.

---

### CWE-681: Incorrect Conversion between Numeric Types
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

`new BigDecimal(body.optDouble("requestedAmount"))` (line 37) converts a `double` (IEEE-754 floating point) to `BigDecimal`. The intermediate `double` representation may lose precision — e.g., `0.1` cannot be represented exactly in binary float. For a loan amount field, this introduces financial calculation errors. The correct approach is `new BigDecimal(body.optString("requestedAmount"))`.

---

### CWE-772: Missing Release of Resource after Effective Lifetime
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

In `insertApplication()` (lines 188–193), the `finally` block closes only the `PreparedStatement` but not the `Connection` itself. If an exception is thrown between `openConnection()` and the method return, the database connection can be leaked. The `Connection` object should be closed in the `finally` block or via try-with-resources.

---

### CWE-775: Missing Release of File Descriptor or Handle after Effective Lifetime
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

The `HttpURLConnection` opened in `callJsonApi()` (line 258) is never explicitly disconnected via `connection.disconnect()`. HTTP connections are held open in the JVM connection pool but the underlying OS socket descriptor is not released until the JVM garbage-collects the `HttpURLConnection` object, which may cause file descriptor exhaustion under high load.

---

### CWE-789: Memory Allocation with Excessive Size Value
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 5
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

In `readBody()`, a `StringBuilder` is grown by repeatedly appending user-supplied request body bytes with no maximum bound. Because the loop reads from a user-controlled `HttpServletRequest` input stream with no `Content-Length` check, an attacker can cause unbounded memory allocation by sending a large request body, potentially triggering an `OutOfMemoryError`.

---

### CWE-1057: Data Access Operations Outside of Expected Data Manager Component
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 5
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java

SQL operations (`INSERT INTO loan_applications`, `UPDATE loan_applications`, `INSERT INTO loan_decisions`) are performed directly inside servlet methods `insertApplication()`, `updateApplication()`, and `insertDecision()` in `LoanApplyServlet`. There is no DAO, Repository, or Service layer. Business logic and data access are co-located in the HTTP request handler, violating the separation of concerns design principle.

---

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/loan-origination.properties, Dockerfile

A plaintext SQL Server password is committed to source control in two locations: (1) loan-origination.properties line 5: `db.****** — packaged into the WAR and loaded at runtime by LoanOriginationConfig; (2) Dockerfile line 11: `ENV DB_PASSWORD=YourStrong!Passw0rd` — baked into the container image layer, visible via docker history/inspect. Both expose the credential to any party with access to the repository or the Docker image.

---

### CWE-732: Incorrect Permission Assignment for Critical Resource
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/loan-origination.properties, Dockerfile

The loan-origination.properties file containing plaintext database credentials (`db.user=sa`, `db.password`) is stored in `src/main/resources` and packaged into the WAR artifact, making it readable by any party who can access the deployed WAR or the repository. Additionally, the Dockerfile bakes these credentials as ENV directives into image layers, where they are accessible to anyone with Docker socket access via `docker inspect` or `docker history`.

---

### CWE-778: Insufficient Logging
- **Category:** Credentials & Secrets
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/loanorigination/LoanApplyServlet.java, src/main/java/com/zavabank/loanorigination/LoanStatusServlet.java

The application has no logging framework (no SLF4J, Log4j, or java.util.logging usage found anywhere in the source). Security-critical events such as KYC failures, risk engine declines, SQL exceptions, and downstream service call failures are not logged — exceptions in `callJsonApi()` are silently swallowed with `catch (Exception ignored)`, and SQL errors are returned as generic HTTP 500 responses with no audit trail. Loan decisions of all types (Approved, Declined, KYC_REVIEW) produce no server-side log entries.

---

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/loan-origination.properties, Dockerfile

Both the SQL Server username and password are committed to source control as plaintext: (1) loan-origination.properties lines 4–5: `db.user=sa` and `db.******; (2) Dockerfile lines 10–11: `ENV DB_USER=sa` and `ENV DB_PASSWORD=YourStrong!Passw0rd`. The account used is the SQL Server system administrator (`sa`), which has maximum privileges over the database instance.
