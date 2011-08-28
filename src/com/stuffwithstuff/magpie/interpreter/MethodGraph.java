package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a collection of methods for a single multimethod and handles
 * sorting and selecting from them efficiently.
 */
public class MethodGraph {
  public MethodGraph() {
    mCache = new HashMap<MethodPair, PatternComparer.Result>();
  }
  
  public Callable select(Context context, Obj arg) {
    Callable selected = null;
    boolean covered[] = new boolean[mMethods.length];
    // TODO(bob): Idea. Instead of using this mutating removed[] to know which
    // methods to ignore, we could do this.
    // When sorting the methods, for each method we generate the list of
    // subsequent *uncovered* methods that follow it if that method matches.
    // Then instead of setting flags to know which methods to skip, we just
    // switch the top level array to walk through that 'remaining uncovered'
    // set.
    for (int i = 0; i < mMethods.length; i++) {
      Callable method = mMethods[i];
      // Don't consider methods covered by one we've already tried.
      if (!covered[i]) {
        // See if this method matches the argument.
        // If the callable has a lexical context, evaluate its pattern in that
        // context. That way pattern names can refer to local variables.
        if (PatternTester.test(context, method.getPattern(),
            arg, method.getClosure())) {
          // Found a match.
          if (selected != null) {
            // Multiple (uncovered) matches, so it's ambiguous.
            throw context.error(Name.AMBIGUOUS_METHOD_ERROR, 
                "Cannot choose a method between " + selected.getPattern() +
                " and " + method.getPattern());
          }

          selected = method;
          for (int cover : mCovering[i]) {
            covered[cover] = true;
          }
        }
      }
    }
    
    // Note: returns null if no method matched.
    return selected;
    
    // TODO(bob): There's an optimization we can do. When doing the topological
    // sort, we can track if a method covers every method that follows it. If
    // it does, then as soon as we check that method, we can stop iterating
    // through the list.
  }
  
  public void refreshGraph(Context context, List<Callable> methods) {
    // Topologically sort the methods so that every method comes before all of
    // the methods it covers.
    List<Callable> sorted = new ArrayList<Callable>();
    
    boolean removed[] = new boolean[methods.size()];
    while (sorted.size() < methods.size()) {
      // Find all of the maximal patterns.
      for (int i = 0; i < methods.size(); i++) {
        // Skip methods we've already sorted.
        if (removed[i]) continue;
        
        Callable callable = methods.get(i);
        boolean isMaximal = true;
        for (int j = 0; j < methods.size(); j++) {
          // Don't compare to self.
          if (i == j) continue;
          
          Callable other = methods.get(j);
          // Skip methods we've already sorted.
          if (removed[j]) continue;
          
          if (compare(context, callable, other) == PatternComparer.Result.LESS) {
            isMaximal = false;
            break;
          }
        }
        
        if (isMaximal) {
          sorted.add(callable);
          // Remove it so that we ignore its outgoing edges now.
          removed[i] = true;
        }
      }
    }
    
    mMethods = sorted.toArray(new Callable[sorted.size()]);
    
    // Cache the list of methods covered by each method.
    mCovering = new int[mMethods.length][];
    for (int i = 0; i < mMethods.length; i++) {
      List<Integer> covering = new ArrayList<Integer>();
      for (int j = i + 1; j < mMethods.length; j++) {
        if (compare(context, mMethods[i], mMethods[j]) ==
            PatternComparer.Result.GREATER) {
          covering.add(j);
        }
      }
      
      mCovering[i] = new int[covering.size()];
      for (int j = 0; j < covering.size(); j++) {
        mCovering[i][j] = covering.get(j);
      }
    }
  }
  
  private PatternComparer.Result compare(Context context, Callable from, Callable to) {
    MethodPair pair = new MethodPair(from, to);
    PatternComparer.Result result = mCache.get(pair);
    if (result == null) {
      result = new PatternComparer(context).compare(from, to);
      mCache.put(pair, result);
      
      // We also know the edge going the other direction.
      MethodPair otherPair = new MethodPair(to, from);
      mCache.put(otherPair, result.reverse());
    }
    
    return result;
  }
  
  private static class MethodPair {
    Callable from;
    Callable to;
    
    public MethodPair(Callable from, Callable to) {
      this.from = from;
      this.to = to;
    }
    
    @Override
    public boolean equals(Object other) {
      MethodPair pair = (MethodPair) other;
      return from == pair.from && to == pair.to;
    }
    
    @Override
    public int hashCode() {
      return from.hashCode() + to.hashCode();
    }
  }

  private final Map<MethodPair, PatternComparer.Result> mCache;
  private Callable[] mMethods;
  private int[][] mCovering;
}
