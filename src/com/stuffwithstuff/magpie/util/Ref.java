package com.stuffwithstuff.magpie.util;

/**
 * A mutable boxed value. Lets you do pass-by-reference arguments to simulate
 * multiple returns.
 *
 * @param <V> The value type.
 */
public class Ref<V> {
  public Ref() {
    mValue = null;
  }
  
  public V get() { return mValue; }
  public void set(V value) { mValue = value; }
  
  private V mValue;
}
