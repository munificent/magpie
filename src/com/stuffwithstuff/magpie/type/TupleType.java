package com.stuffwithstuff.magpie.type;

import java.util.List;

public class TupleType extends TypeDecl {
  public TupleType(List<TypeDecl> fields) {
    mFields = fields;
  }
  
  public List<TypeDecl> getFields() { return mFields; }
  
  private final List<TypeDecl> mFields;
}
