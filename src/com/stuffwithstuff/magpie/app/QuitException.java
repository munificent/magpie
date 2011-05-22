package com.stuffwithstuff.magpie.app;

/**
 * This exception is thrown by the "quit" built-in function to unwind the
 * entire evaluation stack and exit the interpreter.
 */
@SuppressWarnings("serial")
public class QuitException extends RuntimeException {
}