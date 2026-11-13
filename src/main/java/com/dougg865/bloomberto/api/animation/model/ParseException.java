package com.dougg865.bloomberto.api.animation.model;

/** Thrown when a Blockbench file cannot be read. The message says what is wrong and where. */
public final class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
