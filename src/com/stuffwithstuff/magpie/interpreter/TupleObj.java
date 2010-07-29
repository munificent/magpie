package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

public class TupleObj extends Obj {
  public TupleObj(List<Obj> fields) {
    super(null); // TODO(bob): Determine actual type for this.
    mFields = fields;
  }
  
  public List<Obj> getFields() { return mFields; }
  
  private final List<Obj> mFields;
}
