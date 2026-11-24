package com.twitter.ums.contracts;

import java.util.List;

public record SessionCallerResponse(long userId, List<String> roles) {
}
