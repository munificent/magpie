package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.RecordExpr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Built-in callable that takes a record and creates an instance of a class
 * whose fields are initialized with the record's fields.
 */
public class ClassSignify implements Callable {
  public ClassSignify(ClassObj classObj) {
    Expect.notNull(classObj);
    
    mClass = classObj;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    ClassObj classObj = (ClassObj)thisObj;
    
    Obj obj = classObj.instantiate();

    // Assign the declared fields.
    for (Entry<String, FnObj> field : classObj.getFieldDeclarations().entrySet()) {
      Obj value = arg.getField(field.getKey());
      if (value != null) {
        obj.setField(field.getKey(), arg.getField(field.getKey()));
      }
    }

    // Call the initializers for fields that have them.
    for (Entry<String, FnObj> field : classObj.getFieldInitializers().entrySet()) {
      EvalContext fieldContext = new EvalContext(field.getValue().getFunction().getClosure(),
          interpreter.nothing()).pushScope();
      Obj value = interpreter.evaluate(field.getValue().getFunction().getFunction().getBody(),
          fieldContext);
      obj.setField(field.getKey(), value);
    }
    
    // TODO(bob): Needs to call parent constructors too!
    
    return obj;
  }

  public FunctionType getType() {
    // The signify method expects a record with fields for each declared field
    // in the class.
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Entry<String, FnObj> field : mClass.getFieldDeclarations().entrySet()) {
      Expr type = field.getValue().getFunction().getFunction().getBody();
      fields.add(new Pair<String, Expr>(field.getKey(), type));
    }
    
    Expr recordType = new RecordExpr(Position.none(), fields);
    
    return new FunctionType(Collections.singletonList("arg"),
        recordType, Expr.name(mClass.getName()));
  }
  
  private final ClassObj mClass;
}
