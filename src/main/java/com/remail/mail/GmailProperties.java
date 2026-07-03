package com.remail.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "remail.gmail")
public class GmailProperties {

    private boolean enabled;
    private String userId = "me";
    private String query = "in:inbox newer_than:7d";
    private int pollMaxResults = 25;
    private int historyMaxResults = 100;
    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint = "https://oauth2.googleapis.com/token";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPollMaxResults() {
        return pollMaxResults;
    }

    public void setPollMaxResults(int pollMaxResults) {
        this.pollMaxResults = pollMaxResults;
    }

    public int getHistoryMaxResults() {
        return historyMaxResults;
    }

    public void setHistoryMaxResults(int historyMaxResults) {
        this.historyMaxResults = historyMaxResults;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
}