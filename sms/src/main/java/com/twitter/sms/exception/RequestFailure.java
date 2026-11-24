package com.twitter.sms.exception;

public class RequestFailure extends RuntimeException {

    private final int code;
    private final boolean transport;

    private RequestFailure(int code, String message, boolean transport) {
        super(message);
        this.code = code;
        this.transport = transport;
    }

    public static RequestFailure business(int code, String message) {
        return new RequestFailure(code, message, false);
    }

    public static RequestFailure transport(int status, String message) {
        return new RequestFailure(status, message, true);
    }

    public int code() {
        return code;
    }

    public boolean transport() {
        return transport;
    }
}
