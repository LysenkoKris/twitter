package com.twitter.sms.domain;

import java.util.List;

public record Author(long id, List<String> roles) {

    public boolean has(String role) {
        return roles.contains(role);
    }
}
