package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.parser.CharacterReader;
import com.stuffwithstuff.magpie.parser.StringCharacterReader;

public class Module {
  public Module(ModuleInfo info) {
    mInfo = info;
    mScope = new Scope(this);
  }
  
  public String getName() { return mInfo.getName(); }
  public String getPath() { return mInfo.getPath(); }
  public String getSource() { return mInfo.getSource(); }
  public Scope getScope() { return mScope; }
  
  public CharacterReader readSource() {
    return new StringCharacterReader(mInfo.getPath(), mInfo.getSource());

  }
  
  public void exportAll(String prefix, Scope scope) {
    scope.importAll(prefix, mExportedVariables, mExportedMultimethods);
  }
  
  public void exportName(String name, String rename, Scope scope) {
    scope.importName(rename,
        mExportedVariables.get(name), mExportedMultimethods.get(name));
  }
  
  public void addExport(String name, Obj value) {
    if (mExportedVariables.containsKey(name)) throw new IllegalArgumentException();
    
    mExportedVariables.put(name, value);
  }
  
  public void addExport(String name, Multimethod multimethod) {
    mExportedMultimethods.put(name, multimethod);
  }
  
  private final ModuleInfo mInfo;
  private final Scope mScope;
  private final Map<String, Obj> mExportedVariables =
      new HashMap<String, Obj>();
  private final Map<String, Multimethod> mExportedMultimethods =
      new HashMap<String, Multimethod>();
  
}
