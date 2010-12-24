package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.stuffwithstuff.magpie.parser.Position;

public class Profiler {
  public static void display() {
    List<Profile> profiles = new ArrayList<Profile>(sTimes.values());
    Collections.sort(profiles, new Comparator<Profile>() {
      public int compare(Profile e1, Profile e2) {
        long excluded1 = e1.elapsed - e1.excluded;
        long excluded2 = e2.elapsed - e2.excluded;
        return (int)(excluded1 - excluded2);
      }
    });

    String format = "%-40s %6s %10s %10s %10s\n";
    System.out.format(format, "Location", "Calls", "Elapsed ms", "Exclus. ms",
        "Average µs");
    System.out.format(format, "--------", "-----", "----------", "----------",
        "----------");
    for (Profile profile : profiles) {
      long exclusive = profile.elapsed - profile.excluded;
      double average = 1000.0 * exclusive / profile.calls;
      System.out.format(format, profile.label, profile.calls, profile.elapsed,
          exclusive, String.format("%.2f", average));
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
    
    // Update the profile for this function.
    Profile profile = sTimes.get(call.label);
    if (profile == null) {
      profile = new Profile();
      profile.label = call.label;
      sTimes.put(call.label, profile);
    }

    profile.elapsed += elapsed;
    profile.excluded += call.excluded;
    profile.calls++;
    
    // Exclude its time from the caller.
    if (sOngoing.size() > 0) {
      sOngoing.peek().excluded += elapsed;
    }
  }
  
  private static class FunctionCall {
    public String label;
    public long   start;
    public long   excluded;
  }
  
  private static class Profile {
    public String label;
    public long   elapsed;
    public long   excluded;
    public int    calls;
  }
  
  private static final Stack<FunctionCall> sOngoing = new Stack<FunctionCall>();
  private static final Map<String, Profile> sTimes = new HashMap<String, Profile>();
}
