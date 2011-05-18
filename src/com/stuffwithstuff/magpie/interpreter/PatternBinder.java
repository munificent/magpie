package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.pattern.*;

/**
 * Given a pattern, a value, and a context, destructures the value and binds
 * new variables in the context.
 */
public class PatternBinder implements PatternVisitor<Void, Obj>  {
  public static void bind(Context context, boolean isMutable, Pattern pattern,
      Obj value, Scope scope) {
    PatternBinder binder = new PatternBinder(isMutable, context, scope);
    pattern.accept(binder, value);
  }

  @Override
  public Void visit(RecordPattern pattern, Obj value) {
    // Destructure each field.
    for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
      Obj fieldValue = value.getField(field.getKey());
      field.getValue().accept(this, fieldValue);
    }
    
    return null;
  }
    
  @Override
  public Void visit(TypePattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visit(VariablePattern pattern, Obj value) {
    // Bind the variable.
    if (!bindNewVariable(pattern.getName(), value)) {
      // Cannot redefine a variable in the same scope.
      mContext.error(Name.REDEFINITION_ERROR, String.format(
          "There is already a variable named \"%s\" in this scope.", 
          pattern.getName()));
    }
    
    // Recurse into the inner pattern if there is one.
    if (pattern.getPattern() != null) {
      pattern.getPattern().accept(this, value);
    }

    return null;
  }

  @Override
  public Void visit(WildcardPattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  private PatternBinder(boolean isMutable, Context context, Scope scope) {
    mIsMutable = isMutable;
    mContext = context;
    mScope = scope;
  }
  
  private boolean bindNewVariable(String name, Obj value) {
    // Ignore the wildcard name.
    if (name.equals("_")) return true;
    
    // Bind the variable.
    return mScope.define(mIsMutable, name, value);
  }
  
  private final boolean mIsMutable;
  private final Context mContext;  
  private final Scope mScope;
}
