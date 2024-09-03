package com.zuomagai.spin.parser;

public class SpinParseException extends RuntimeException {

    public SpinParseException(String message) {
        super(message);
    }

    public SpinParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
