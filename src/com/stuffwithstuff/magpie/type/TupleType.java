package com.stuffwithstuff.magpie.type;

import java.util.*;

public class TupleType extends TypeDecl {
  public TupleType(List<TypeDecl> fields) {
    mFields = fields;
  }
  
  public TupleType(TypeDecl... fields) {
    mFields = Arrays.asList(fields);
  }
  
  public List<TypeDecl> getFields() { return mFields; }
  
  private final List<TypeDecl> mFields;
}
