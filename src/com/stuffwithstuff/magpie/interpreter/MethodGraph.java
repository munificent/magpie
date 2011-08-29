package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a collection of methods for a single multimethod and handles
 * sorting and selecting from them efficiently. The goal of this class is,
 * given an argument, find the best method that matches it. If multiple best
 * methods match (i.e. multiple methods match which can't be compared), then
 * signal an error. If no method matches, indicate that too.
 * 
 * Given:
 * 
 * Any two patterns A and B can be compared. If A is a more specific pattern
 * than be (for example, it's a type pattern of a subclass and B is a type
 * pattern for a superclass), then A is "less" than B, and B is "greater" than
 * A. We also say that A "covers" B, meaning that if an argument matches A then
 * there's no need to consider B since A would always be preferred.
 * 
 * It's also possible for two patterns to be equal (which should be an error
 * when the method is defined, so we don't consider it here) or "non-compared"
 * meaning there is no ordering between methods. Unlike other multimethod
 * linearization systems, in Magpie not all methods can be ordered relative to
 * each other. Instead of a total order, methods form a partially ordered set.
 * 
 * There are two phases of the algorithm, a definition time phase and a
 * dispatch-time phase. The definition-time one only needs to be run when the
 * set of methods has changed. It can be relatively slow. It's job is to
 * precalculate as much as it can. The dispatch-time phase happens every time
 * a method is invoked. It's job is to pick a method as quickly as possible.
 * 
 * Definition phase:
 * 
 * First we get all methods and topologically sort them. This results in an
 * array where every method appears in the array after any method that covers
 * it. (In other words, the array is ordered from most specific to least).
 * Because methods are a poset, there are multiple valid topological orderings.
 * The one we pick doesn't affect the results, so we just pick one arbitrarily.
 * 
 * With this, we can test methods in best-to-worst order. Now we need to handle
 * patterns covering others. We walk this array. For each method, we go through
 * the rest of the array and see which methods it covers. We get that list of
 * remaining uncovered methods and cache that as an array with that method. So,
 * for each method, we cache the set of other methods that need to be tested if
 * that method matches.
 * 
 * Dispatch phase:
 * 
 * When a method is invoked, we walk the array of sorted methods. As soon as we
 * find a match (which is, by definition, the best match), we store it. Now we
 * just need to check for another equally best match, in case the method is
 * ambiguous. We now walk through the matched methods remaining array that we
 * cached above. If any method in that matches, we have an error. If we reach
 * the end with no match, then the single match we stored above wins.
 * 
 * If we get all the way through the entire method array with no match, we just
 * return null to indicate that.
 */
public class MethodGraph {
  public MethodGraph() {
    mCache = new HashMap<MethodPair, PatternComparer.Result>();
  }
  
  public Callable select(Context context, Obj arg) {
    Callable selected = null;

    Callable[] methods = mMethods; 
    for (int i = 0; i < methods.length;) {
      Callable method = methods[i];
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
        
        // This method has matched, so only search the remaining methods that
        // it doesn't cover.
        methods = mRemaining[i];
        i = 0;
      } else {
        i++;
      }
    }
    
    // Note: returns null if no method matched.
    return selected;
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
    
    // For each method, calculate the list of remaining methods that need to be
    // tested after that method matches.
    mRemaining = new Callable[mMethods.length][];
    for (int i = 0; i < mMethods.length; i++) {
      List<Callable> remaining = new ArrayList<Callable>();
      for (int j = i + 1; j < mMethods.length; j++) {
        if (compare(context, mMethods[i], mMethods[j]) !=
            PatternComparer.Result.GREATER) {
          remaining.add(mMethods[j]);
        }
      }
      
      mRemaining[i] = new Callable[remaining.size()];
      for (int j = 0; j < remaining.size(); j++) {
        mRemaining[i][j] = remaining.get(j);
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
  private Callable[][] mRemaining;
}
