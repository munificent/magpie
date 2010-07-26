package com.stuffwithstuff.magpie;

@SuppressWarnings("serial")
public class ParseError extends Error {
    public ParseError(final String message) {
        super(message);
    }
}
