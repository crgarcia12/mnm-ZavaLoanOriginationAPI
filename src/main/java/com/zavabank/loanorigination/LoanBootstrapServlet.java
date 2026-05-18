package com.zavabank.loanorigination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class LoanBootstrapServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = LoanConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "IF OBJECT_ID('LoanApplications', 'U') IS NULL " +
                    "CREATE TABLE LoanApplications (" +
                    "ApplicationID INT IDENTITY(1,1) PRIMARY KEY, " +
                    "CustomerID INT NOT NULL, " +
                    "LoanProductID INT NOT NULL, " +
                    "RequestedAmount DECIMAL(18,2) NOT NULL, " +
                    "ApprovedAmount DECIMAL(18,2) NULL, " +
                    "InterestRate DECIMAL(5,4) NULL, " +
                    "TermMonths INT NOT NULL, " +
                    "Purpose NVARCHAR(500) NULL, " +
                    "Status NVARCHAR(30) NOT NULL DEFAULT 'Submitted', " +
                    "ApplicationDate DATETIME NOT NULL DEFAULT GETDATE(), " +
                    "DecisionDate DATETIME NULL, " +
                    "DecisionNotes NVARCHAR(MAX) NULL, " +
                    "LoanAccountID INT NULL, " +
                    "CreatedDate DATETIME NOT NULL DEFAULT GETDATE(), " +
                    "ModifiedDate DATETIME NOT NULL DEFAULT GETDATE()" +
                    "); " +
                    "IF OBJECT_ID('LoanDecisions', 'U') IS NULL " +
                    "CREATE TABLE LoanDecisions (" +
                    "DecisionID INT IDENTITY(1,1) PRIMARY KEY, " +
                    "ApplicationID INT NOT NULL, " +
                    "DecisionType NVARCHAR(20) NOT NULL, " +
                    "DecisionBy NVARCHAR(100) NULL, " +
                    "DecisionDate DATETIME NOT NULL DEFAULT GETDATE(), " +
                    "Reason NVARCHAR(MAX) NULL, " +
                    "CreditScoreAtTime INT NULL, " +
                    "RiskLevel NVARCHAR(10) NULL, " +
                    "Conditions NVARCHAR(MAX) NULL" +
                    ");"
            );
            statement.execute();
        } catch (SQLException exception) {
            throw new ServletException("Loan bootstrap failed.", exception);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
