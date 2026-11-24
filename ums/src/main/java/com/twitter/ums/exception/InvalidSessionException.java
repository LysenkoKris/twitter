package com.twitter.ums.exception;

public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException() {
        super("invalid session");
    }
}
