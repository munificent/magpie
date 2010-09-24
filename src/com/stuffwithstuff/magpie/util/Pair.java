package com.stuffwithstuff.magpie.util;

/**
 * A simple immutable key/value pair.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class Pair<K, V> {
  public Pair(K key, V value) {
    mKey = key;
    mValue = value;
  }
  
  public K getKey() { return mKey; }
  public V getValue() { return mValue; }
  
  private final K mKey;
  private final V mValue;
}
