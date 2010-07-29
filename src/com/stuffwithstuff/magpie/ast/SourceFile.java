package com.stuffwithstuff.magpie.ast;

import java.util.List;

/**
 * Root AST node for an entire source file.
 */
public class SourceFile {
  public SourceFile(List<FunctionDefn> functions) {
    mFunctions = functions;
  }
  
  public List<FunctionDefn> getFunctions() { return mFunctions; }
  
  private final List<FunctionDefn> mFunctions;

}
