package com.zavabank.loanorigination;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class LoanApplyServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject payload;
        try {
            payload = new JSONObject(readBody(request));
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Invalid JSON payload.\"}");
            return;
        }

        int customerId = payload.optInt("customerId", 0);
        int loanProductId = payload.optInt("loanProductId", 0);
        BigDecimal requestedAmount = new BigDecimal(payload.optDouble("requestedAmount", 0d));
        int termMonths = payload.optInt("termMonths", 0);
        String purpose = payload.optString("purpose", "").trim();
        String fullName = payload.optString("fullName", "").trim();
        String idNumber = payload.optString("idNumber", "").trim();

        if (customerId <= 0 || loanProductId <= 0 || requestedAmount.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"customerId, loanProductId, requestedAmount and termMonths are required.\"}");
            return;
        }
        if (fullName.length() == 0) {
            fullName = "Customer " + customerId;
        }

        Connection connection = null;
        try {
            connection = LoanConnectionFactory.openConnection();
            connection.setAutoCommit(false);

            long applicationId = insertApplication(connection, customerId, loanProductId, requestedAmount, termMonths, purpose);
            OrchestrationResult orchestration = orchestrateDecision(applicationId, payload, customerId, requestedAmount, fullName, idNumber);

            updateApplication(connection, applicationId, orchestration, termMonths);
            insertDecision(connection, applicationId, orchestration);
            connection.commit();

            JSONObject result = new JSONObject();
            result.put("status", orchestration.status);
            result.put("applicationId", applicationId);
            result.put("kycStatus", orchestration.kycStatus);
            result.put("riskScore", orchestration.riskScore);
            result.put("riskLevel", orchestration.riskLevel);
            result.put("approvedAmount", orchestration.approvedAmount);
            result.put("ledgerAccountCreated", orchestration.ledgerCreated);
            result.put("ledgerAccountId", orchestration.ledgerAccountId);
            response.getWriter().write(result.toString());
        } catch (SQLException exception) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to submit loan application.\"}");
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private OrchestrationResult orchestrateDecision(
        long applicationId,
        JSONObject payload,
        int customerId,
        BigDecimal requestedAmount,
        String fullName,
        String idNumber
    ) {
        OrchestrationResult result = new OrchestrationResult();
        result.status = "Submitted";
        result.approvedAmount = BigDecimal.ZERO;
        result.interestRate = BigDecimal.valueOf(LoanOriginationConfig.getDefaultInterestRate());

        JSONObject kycPayload = new JSONObject();
        kycPayload.put("customerId", customerId);
        kycPayload.put("fullName", fullName);
        kycPayload.put("idNumber", idNumber);
        JSONObject kycResponse = callJsonApi(LoanOriginationConfig.getKycServiceBaseUrl() + "/api/kyc/verify", kycPayload);
        result.kycStatus = kycResponse.optString("status", "REVIEW");
        if (!"PASS".equalsIgnoreCase(result.kycStatus)) {
            result.status = "KYC_REVIEW";
            result.decisionReason = "KYC verification requires review.";
            return result;
        }

        JSONObject riskPayload = new JSONObject();
        riskPayload.put("applicationId", applicationId);
        riskPayload.put("customerId", customerId);
        riskPayload.put("requestedAmount", requestedAmount);
        riskPayload.put("termMonths", payload.optInt("termMonths", 12));
        JSONObject riskResponse = callJsonApi(LoanOriginationConfig.getRiskServiceBaseUrl() + "/api/risk/score", riskPayload);

        result.riskScore = riskResponse.optInt("score", 0);
        result.riskLevel = riskResponse.optString("riskLevel", result.riskScore >= 700 ? "Low" : (result.riskScore >= 620 ? "Medium" : "High"));
        boolean approved = riskResponse.has("approved") ? riskResponse.optBoolean("approved", false) : result.riskScore >= 620;
        result.approvedAmount = riskResponse.has("approvedAmount")
            ? BigDecimal.valueOf(riskResponse.optDouble("approvedAmount", requestedAmount.doubleValue()))
            : requestedAmount;
        result.interestRate = riskResponse.has("interestRate")
            ? BigDecimal.valueOf(riskResponse.optDouble("interestRate", LoanOriginationConfig.getDefaultInterestRate()))
            : BigDecimal.valueOf(LoanOriginationConfig.getDefaultInterestRate());

        if (!approved) {
            result.status = "Declined";
            result.decisionReason = "Risk engine declined the application.";
            return result;
        }

        JSONObject ledgerPayload = new JSONObject();
        ledgerPayload.put("customerId", customerId);
        ledgerPayload.put("accountTypeId", payload.optInt("loanAccountTypeId", 2));
        ledgerPayload.put("openingBalance", 0);
        ledgerPayload.put("description", "Loan account for application " + applicationId);
        JSONObject ledgerResponse = callJsonApi(LoanOriginationConfig.getLedgerServiceBaseUrl() + "/api/accounts/create", ledgerPayload);
        result.ledgerAccountId = ledgerResponse.optLong("accountId", 0L);
        result.ledgerCreated = ledgerResponse.optBoolean("created", result.ledgerAccountId > 0);
        result.status = result.ledgerCreated ? "Approved" : "APPROVED_PENDING_LEDGER";
        result.decisionReason = result.ledgerCreated
            ? "Approved by risk engine and posted to ledger."
            : "Approved by risk engine; ledger account creation pending.";
        return result;
    }

    private long insertApplication(
        Connection connection,
        int customerId,
        int loanProductId,
        BigDecimal requestedAmount,
        int termMonths,
        String purpose
    ) throws SQLException {
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            statement = connection.prepareStatement(
                "INSERT INTO LoanApplications (CustomerID, LoanProductID, RequestedAmount, TermMonths, Purpose, Status, ApplicationDate, CreatedDate, ModifiedDate) " +
                    "VALUES (?, ?, ?, ?, ?, 'Submitted', GETDATE(), GETDATE(), GETDATE())",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setInt(1, customerId);
            statement.setInt(2, loanProductId);
            statement.setBigDecimal(3, requestedAmount);
            statement.setInt(4, termMonths);
            statement.setString(5, purpose);
            statement.executeUpdate();
            keys = statement.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Loan application ID was not generated.");
        } finally {
            if (keys != null) {
                keys.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void updateApplication(Connection connection, long applicationId, OrchestrationResult result, int termMonths) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                "UPDATE LoanApplications SET Status = ?, ApprovedAmount = ?, InterestRate = ?, DecisionDate = GETDATE(), " +
                    "DecisionNotes = ?, LoanAccountID = ?, ModifiedDate = GETDATE() WHERE ApplicationID = ?"
            );
            statement.setString(1, result.status);
            statement.setBigDecimal(2, "Declined".equalsIgnoreCase(result.status) ? null : result.approvedAmount);
            statement.setBigDecimal(3, result.interestRate);
            statement.setString(4, result.decisionReason + " TermMonths=" + termMonths + ", KYC=" + result.kycStatus + ", Risk=" + result.riskLevel);
            if (result.ledgerAccountId > 0) {
                statement.setLong(5, result.ledgerAccountId);
            } else {
                statement.setNull(5, java.sql.Types.INTEGER);
            }
            statement.setLong(6, applicationId);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void insertDecision(Connection connection, long applicationId, OrchestrationResult result) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                "INSERT INTO LoanDecisions (ApplicationID, DecisionType, DecisionBy, DecisionDate, Reason, CreditScoreAtTime, RiskLevel, Conditions) " +
                    "VALUES (?, ?, 'LoanOriginationAPI', GETDATE(), ?, ?, ?, ?)"
            );
            String decisionType = "Approved".equalsIgnoreCase(result.status) || "APPROVED_PENDING_LEDGER".equalsIgnoreCase(result.status)
                ? "Approve"
                : ("KYC_REVIEW".equalsIgnoreCase(result.status) ? "ManualReview" : "Decline");
            statement.setLong(1, applicationId);
            statement.setString(2, decisionType);
            statement.setString(3, result.decisionReason);
            statement.setInt(4, result.riskScore);
            statement.setString(5, result.riskLevel);
            statement.setString(6, result.ledgerCreated ? "Ledger account created." : "Ledger account pending.");
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private JSONObject callJsonApi(String endpoint, JSONObject payload) {
        HttpURLConnection connection = null;
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setDoOutput(true);

            writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(payload.toString());
            writer.flush();

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                reader = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    body.append(line);
                    line = reader.readLine();
                }
                if (body.length() == 0) {
                    return new JSONObject();
                }
                return new JSONObject(body.toString());
            }
        } catch (Exception ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new JSONObject();
    }

    private String readBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder body = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            body.append(line);
            line = reader.readLine();
        }
        return body.toString();
    }

    private static class OrchestrationResult {
        private String status;
        private String kycStatus;
        private int riskScore;
        private String riskLevel;
        private BigDecimal approvedAmount;
        private BigDecimal interestRate;
        private long ledgerAccountId;
        private boolean ledgerCreated;
        private String decisionReason;
    }
}
