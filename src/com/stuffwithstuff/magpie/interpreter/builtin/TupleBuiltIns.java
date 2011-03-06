package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.Position;

public class TupleBuiltIns {
  @Getter("type Type")
  public static class Type implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // The type of a tuple is a tuple of the types of its fields.
      int numFields = thisObj.getField(Name.COUNT).asInt();
      Obj[] fieldTypes = new Obj[numFields];
      for (int i = 0; i < numFields; i++) {
        // TODO(bob): Type should be moved into a namespace.
        Obj fieldType = interpreter.getQualifiedMember(
            Position.none(), thisObj.getTupleField(i), thisObj, Name.TYPE);
        fieldTypes[i] = fieldType;
      }
      
      return interpreter.createTuple(fieldTypes);
    }
  }
}
