package com.zavabank.loanorigination;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class LoanOriginationConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            InputStream inputStream = LoanOriginationConfig.class.getClassLoader().getResourceAsStream("loan-origination.properties");
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
                inputStream.close();
            }
        } catch (IOException ignored) {
        }
    }

    private LoanOriginationConfig() {
    }

    public static String getDbUrl() {
        String host = read("DB_HOST", "db.host");
        String port = read("DB_PORT", "db.port");
        String name = read("DB_NAME", "db.name");
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name + ";encrypt=false;trustServerCertificate=true";
    }

    public static String getDbUser() {
        return read("DB_USER", "db.user");
    }

    public static String getDbPassword() {
        return read("DB_PASSWORD", "db.password");
    }

    public static String getKycServiceBaseUrl() {
        return read("KYC_SERVICE_BASE_URL", "kyc.service.baseUrl");
    }

    public static String getRiskServiceBaseUrl() {
        return read("RISK_SERVICE_BASE_URL", "risk.service.baseUrl");
    }

    public static String getLedgerServiceBaseUrl() {
        return read("LEDGER_SERVICE_BASE_URL", "ledger.service.baseUrl");
    }

    public static double getDefaultInterestRate() {
        try {
            return Double.parseDouble(read("LOAN_DEFAULT_INTEREST_RATE", "loan.default.interestRate"));
        } catch (Exception ignored) {
            return 0.0725d;
        }
    }

    private static String read(String envKey, String propertyKey) {
        String value = System.getenv(envKey);
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        }
        return PROPERTIES.getProperty(propertyKey);
    }
}
