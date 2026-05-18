package com.zavabank.loanorigination;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class LoanStatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Expected /api/loans/{id}/status.\"}");
            return;
        }

        String[] segments = pathInfo.split("/");
        if (segments.length != 3 || !"status".equalsIgnoreCase(segments[2])) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Expected /api/loans/{id}/status.\"}");
            return;
        }

        long applicationId;
        try {
            applicationId = Long.parseLong(segments[1]);
        } catch (NumberFormatException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Loan application id must be numeric.\"}");
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = LoanConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "SELECT ApplicationID, CustomerID, Status, RequestedAmount, ApprovedAmount, InterestRate, TermMonths, " +
                    "DecisionDate, DecisionNotes, LoanAccountID FROM LoanApplications WHERE ApplicationID = ?"
            );
            statement.setLong(1, applicationId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"status\":\"NOT_FOUND\",\"message\":\"Loan application not found.\"}");
                return;
            }

            JSONObject result = new JSONObject();
            result.put("applicationId", resultSet.getLong("ApplicationID"));
            result.put("customerId", resultSet.getInt("CustomerID"));
            result.put("status", resultSet.getString("Status"));
            result.put("requestedAmount", resultSet.getBigDecimal("RequestedAmount"));
            result.put("approvedAmount", resultSet.getBigDecimal("ApprovedAmount"));
            result.put("interestRate", resultSet.getBigDecimal("InterestRate"));
            result.put("termMonths", resultSet.getInt("TermMonths"));
            result.put("decisionDate", resultSet.getTimestamp("DecisionDate"));
            result.put("decisionNotes", resultSet.getString("DecisionNotes"));
            result.put("loanAccountId", resultSet.getObject("LoanAccountID"));
            response.getWriter().write(result.toString());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to load loan status.\"}");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
