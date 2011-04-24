package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class ImportExpr extends Expr {
  /**
   * Creates a new ImportExpr.
   * 
   * @param module  The name of the module to import. Will start with "." for
   *                a relative import.
   * @param name    The name to import from the module, or null if all names
   *                should be imported.
   * @param rename  The name that the imported name should be imported as. If
   *                name is null, than this is a prefix that will be applied to
   *                all imported names (with a "." added). If "_", the module
   *                will be used as a prefix.
   */
  ImportExpr(Position position, String scheme, String module, String name, String rename) {
    super(position);
    mScheme = scheme;
    mModule = module;
    mName   = name;
    mRename = rename;
  }
  
  public String getScheme() { return mScheme; }
  public String getModule() { return mModule; }
  public String getName() { return mName; }
  public String getRename() { return mRename; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("import ").append(mModule);
    
    if (mScheme != null) builder.append(mScheme).append(":");
    if (mName != null) builder.append(" ").append(mName);
    if (mRename != null) builder.append(" = ").append(mRename);
    
    builder.append("\n");
  }

  private final String mScheme;
  private final String mModule;
  private final String mName;
  private final String mRename;
}
