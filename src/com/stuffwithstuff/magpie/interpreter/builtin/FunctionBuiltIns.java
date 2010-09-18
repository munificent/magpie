package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FunctionBuiltIns {
  @Signature("type()")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)thisObj;
    
    // Evaluate the function's type annotation in the context of its closure so
    // that the type can reference static type arguments in effect where the
    // function was defined.
    // TODO: There's a problem here. We're ignoring the static type arguments of
    // this function itself. Given:
    //     var foo[A](arg A)
    // This tries to evaluate "arg A" without first binding A to anything, which
    // of course fails. There are two questions here:
    // 1. What *is* the type of an uninstantiated generic function?
    //    In other words, if you do:
    //    
    //    var foo[A](arg A)
    //    var b = foo type
    //
    //    What should b contain?
    //
    //    A: Magpie doesn't instantiate generic functions. There are just
    //    functions. In this case, it's type is [A](A), a function that takes
    //    one static argument with no constraint, one dynamic argument of that
    //    type and returns a Dynamic.
    //
    //    That implies that we can't know the concrete dynamic argument type
    //    until after we've applied the static argument. That means the dynamic
    //    argument and return types in a function type object really need to be
    //    expressions and not evaluated objects if there is a static argument.
    //
    // 2. When is that type evaluated and constructed?
    //    The answer here is that we evaluate the function's type annotation
    //    every time you ask for its type. My hunch is that a better solution is
    //    to evaluate it when the function is defined and then "type" just
    //    returns that.
    EvalContext context = new EvalContext(function.getClosure(),
        interpreter.nothing());
    return interpreter.evaluateFunctionType(function.getType(), context);
  }
}
