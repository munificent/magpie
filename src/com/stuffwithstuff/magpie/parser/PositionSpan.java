package com.stuffwithstuff.magpie.parser;

/**
 * Helper class for creating a position that spans a range of Tokens.
 */
public class PositionSpan {
  public PositionSpan(Parser parser, Position start) {
    mParser = parser;
    mStart  = start;
  }
  
  public Position end() {
    return mStart.union(mParser.last(1).getPosition());
  }
  
  private final Parser mParser;
  private final Position     mStart;
}
