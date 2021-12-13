package com.modusbox.client.processor;

import java.time.*;
import java.time.temporal.ChronoUnit;

public class TokenStore {

    //////////////////// Access Token methods ////////////////////
    public static String accessToken = "";
    public static Instant accessTokenExpirationTimestamp = Instant.now();

    public String getAccessToken() {
        if (hasAccessTokenExpired()){
            return "";
        }
        return accessToken;
    }

    //  Format of t is UUID: "083464d7-a606-4423-a2fb-fc3ce0de8abe"
    //  Format of e is in hours: "1h"
    public void setAccessToken(String t, String e) {
        accessToken = t;
        accessTokenExpirationTimestamp = Instant.now().plus(Long.parseLong((e.substring(0, e.length() - 1))), ChronoUnit.HOURS);
    }

    public boolean hasAccessTokenExpired() {
        if (accessToken.equals("")) {
            return true;
        } else if (accessTokenExpirationTimestamp.isBefore(Instant.now())) {
            return true;
        }
        return false;
    }

}