package com.zavabank.loanorigination;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class LoanConnectionFactory {
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("SQL Server JDBC driver not found", exception);
        }
    }

    private LoanConnectionFactory() {
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            LoanOriginationConfig.getDbUrl(),
            LoanOriginationConfig.getDbUser(),
            LoanOriginationConfig.getDbPassword()
        );
    }
}
