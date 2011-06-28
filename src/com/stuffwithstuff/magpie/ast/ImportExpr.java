package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

public class ImportExpr extends Expr {
  /**
   * Creates a new ImportExpr.
   * 
   * @param module  The name of the module to import. Will start with "." for
   *                a relative import.
   */
  ImportExpr(Position position, String scheme, String module,
      String prefix, boolean isOnly, List<ImportDeclaration> declarations) {
    super(position);
    mScheme = scheme;
    mModule = module;
    mPrefix = prefix;
    mIsOnly = isOnly;
    mDeclarations = declarations;
  }
  
  public String getScheme() { return mScheme; }
  public String getModule() { return mModule; }
  public String getPrefix() { return mPrefix; }
  public boolean isOnly() { return mIsOnly; }
  public List<ImportDeclaration> getDeclarations() { return mDeclarations; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("import ");
    
    if (mScheme != null) builder.append(mScheme).append(":");
    builder.append(mModule);
    
    // TODO(bob): Update.
    
    builder.append("\n");
  }

  private final String mScheme;
  private final String mModule;
  private final String mPrefix;
  private final boolean mIsOnly;
  private final List<ImportDeclaration> mDeclarations;
}
