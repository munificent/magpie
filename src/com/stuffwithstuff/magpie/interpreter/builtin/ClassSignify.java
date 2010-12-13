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
import com.stuffwithstuff.magpie.interpreter.Field;
import com.stuffwithstuff.magpie.interpreter.Function;
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
    
    Obj obj = interpreter.instantiate(classObj, null);

    // Initialize or assign the fields.
    for (Entry<String, Field> field : classObj.getFieldDefinitions().entrySet()) {
      if (!field.getValue().hasInitializer()) {
        // Assign it from the record.
        Obj value = arg.getField(field.getKey());
        if (value != null) {
          obj.setField(field.getKey(), arg.getField(field.getKey()));
        }
      }
    }
    
    // TODO(bob): Needs to call parent constructors too!
    
    return obj;
  }

  public Obj getType(Interpreter interpreter) {
    // The signify method expects a record with fields for each declared field
    // in the class.
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Entry<String, Field> field : mClass.getFieldDefinitions().entrySet()) {
      if (!field.getValue().hasInitializer()) {
        Callable typeDeclaration = field.getValue().getDefinition();
        // TODO(bob): This cast here is gross!
        Expr type = ((Function)typeDeclaration).getFunction().getBody();
        fields.add(new Pair<String, Expr>(field.getKey(), type));
      }
    }

    Expr recordType = new RecordExpr(Position.none(), fields);
    
    FunctionType type = new FunctionType(Collections.singletonList("arg"),
        recordType, Expr.name(mClass.getName()), false);
    
    return interpreter.evaluateFunctionType(type, null);
  }
  
  private final ClassObj mClass;
}
