package com.stuffwithstuff.magpie.interpreter;

/**
 * This "exception" is used internally by the evaluator to implement "break" in
 * loop expressions. A break expression will throw this, which will be caught
 * by the evaluation of the outer loop expression to exit the loop.
 */
@SuppressWarnings("serial")
public class BreakException extends RuntimeException {
}