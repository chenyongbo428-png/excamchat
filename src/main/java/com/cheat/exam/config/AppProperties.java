package com.cheat.exam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Storage storage = new Storage();
    private final Model model = new Model();

    public Security getSecurity() {
        return security;
    }

    public Storage getStorage() {
        return storage;
    }

    public Model getModel() {
        return model;
    }

    public static final class Security {
        private long tokenExpirationSeconds = 7200;

        public long getTokenExpirationSeconds() {
            return tokenExpirationSeconds;
        }

        public void setTokenExpirationSeconds(long tokenExpirationSeconds) {
            this.tokenExpirationSeconds = tokenExpirationSeconds;
        }
    }

    public static final class Storage {
        private String uploadDir = "storage/uploads";

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }
    }

    public static final class Model {
        private final Bailian bailian = new Bailian();

        public Bailian getBailian() {
            return bailian;
        }
    }

    public static final class Bailian {
        private String apiKey;
        private String baseUrl;
        private int timeoutSeconds = 60;
        private int maxTokens = 700;
        private double temperature = 0.2;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
