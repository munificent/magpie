package com.stuffwithstuff.magpie.parser;

/**
 * Describes the location of a piece of text in a source file.
 */
public class Position {
  public static Position none() {
    return new Position("", -1, -1, -1, -1);
  }
  
  public Position(String sourceFile, int startLine, int startCol,
      int endLine, int endCol) {
    mSourceFile = sourceFile;
    mStartLine = startLine;
    mStartCol = startCol;
    mEndLine = endLine;
    mEndCol = endCol;
  }
  
  public Position union(Position other) {
    int startLine = Math.min(mStartLine, other.mStartLine);
    int startCol = Math.min(mStartCol, other.mStartCol);
    int endLine = Math.max(mEndLine, other.mEndLine);
    int endCol = Math.max(mEndCol, other.mEndCol);
    
    return new Position(mSourceFile, startLine, startCol,
        endLine, endCol);
  }
  
  public String getSourceFile() { return mSourceFile; }
  public int getStartLine() { return mStartLine; }
  public int getStartCol() { return mStartCol; }
  public int getEndLine() { return mEndLine; }
  public int getEndCol() { return mEndCol; }
  
  public String toString() {
    if (mStartLine == -1) {
      return "(Unknown position)";
    }
    
    if (mStartLine == mEndLine) {
      return String.format("%s (line %d, col %d-%d)", mSourceFile, mStartLine,
          mStartCol, mEndCol);
    }
    
    return String.format("%s (line %d col %d - line %d col %d)", mSourceFile,
        mStartLine, mStartCol, mEndLine, mEndCol);
  }
  
  private final String mSourceFile;
  private final int mStartLine;
  private final int mStartCol;
  private final int mEndLine;
  private final int mEndCol;
}
