package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.Position;

public class RecordBuiltIns {  
  @Getter("type(-> Type)")
  public static class Type implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // Much like a tuple, the type of a record is a record of the types of its
      // fields.
      Map<String, Obj> fieldTypes = new HashMap<String, Obj>();
      
      for (Entry<String, Obj> field : thisObj.getFields().entries()) {
        Obj fieldType = interpreter.getMember(Position.none(), field.getValue(),
            Identifiers.TYPE);
        fieldTypes.put(field.getKey(), fieldType);
      }
      
      return interpreter.createRecord(fieldTypes);
    }
  }
}
