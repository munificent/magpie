package com.stuffwithstuff.magpie.util;

/**
 * A simple function.
 *
 * @param <A> The argument type.
 * @param <R> The return type.
 */
public interface Fn<A, R> {
  R apply(A arg);
}
