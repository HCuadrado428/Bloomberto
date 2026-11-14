package com.dougg865.bloomberto.api.animation.molang;

/** An expression that cannot be parsed. */
public final class MolangException extends Exception {
    public MolangException(String message) {
        super(message);
    }
}
