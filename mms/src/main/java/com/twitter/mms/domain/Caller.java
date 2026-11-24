package com.twitter.mms.domain;

import java.util.List;

public record Caller(long userId, List<String> roles) {

    public boolean has(String role) {
        return roles.contains(role);
    }
}
