package com.fintech.core.common.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Seed seed = new Seed();
    private final Cors cors = new Cors();
    private final Limits limits = new Limits();
    private final Upload upload = new Upload();
    private final Account account = new Account();
    private final Password password = new Password();

    @Data
    public static class Seed {
        private final Admin admin = new Admin();

        @Data
        public static class Admin {
            private String email = "admin@fintech.local";
            private String password = "Admin@123";
            private String firstName = "System";
            private String lastName = "Admin";
            private String phone = "01062204741";
        }
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000,http://localhost:5173,http://fintech-react-s3-bucket-v1.s3-website.eu-north-1.amazonaws.com";
    }

    @Data
    public static class Limits {
        private BigDecimal dailyOutgoing = new BigDecimal("50000.00");
        private BigDecimal maxSingleAmount = new BigDecimal("10000.00");
    }

    @Data
    public static class Upload {
        private String profileDir = "./uploads/profile-picture";
        /** Max profile picture size in bytes (default 5MB). */
        private long maxFileSizeBytes = 5L * 1024 * 1024;
    }

    @Data
    public static class Account {
        private String numberPrefix = "66";
        private int numberLength = 8;
    }

    @Data
    public static class Password {
        private final Reset reset = new Reset();

        @Data
        public static class Reset {
            private String link = "http://localhost:3000/reset-password?code=";
            private int expiryHours = 5;
        }
    }
}
