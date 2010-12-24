package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.stuffwithstuff.magpie.parser.Position;

public class Profiler {
  public static void display() {
    String format = "%-40s %10s %12s\n";
    System.out.format(format, "Location", "Calls", "Elapsed (ms)");
    System.out.format(format, "--------", "-----", "------------");
    for (Profile profile : sTimes.values()) {
      System.out.format(format, profile.label, profile.calls, profile.elapsed);
    }
  }
  
  public static void push(Position position) {
    FunctionCall call = new FunctionCall();
    call.label = position.getSourceFile() + ":" + position.getStartLine();
    call.start = System.currentTimeMillis();
    sOngoing.push(call);
  }
  
  public static void pop() {
    FunctionCall call = sOngoing.pop();
    long elapsed = System.currentTimeMillis() - call.start;
    
    Profile profile = sTimes.get(call.label);
    if (profile == null) {
      profile = new Profile();
      profile.label = call.label;
      sTimes.put(call.label, profile);
    }

    profile.elapsed = profile.elapsed + elapsed;
    profile.calls++;
  }
  
  private static class FunctionCall {
    public String label;
    public long   start;
  }
  
  private static class Profile {
    public String label;
    public long   elapsed;
    public int    calls;
  }
  
  private static final Stack<FunctionCall> sOngoing = new Stack<FunctionCall>();
  private static final Map<String, Profile> sTimes = new HashMap<String, Profile>();
}
