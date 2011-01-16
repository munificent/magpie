^title What Is the Type of a Generic?

Consider this function:

    var a[T](arg T ->)
   
Normally, the type of a function is an instance of `FunctionType`. It has fields for the declared parameter and return types. Both fields are expected to contain types. `FunctionType` is itself a type: it implements `canAssignFrom()` and `getMethodType()`.

The problem is that a function type that has a static parameter isn't the same as a regular function type. `FunctionType` implements `getMethodType()` to handle the param and return type for `call`, which is how you invoke a function. A generic function can't do that since it doesn't know the concrete types `call` expects.

Instead, a `GenericFunctionType` stores thunks for the parameter and return types. Those thunks need to be evaluated *after* a static argument is applied before you can get the type of the object's `call` method. So maybe there is a two-stage process: A generic function supports a single operation: "instantiate" that takes the static arguments. That returns a concrete function object with concrete parameter and return types.

That should also simplify evaluation. We're essentially currying the static argument, so we don't have to pass it around everywhere in addition to the dynamic argument. Instead, the static argument is applied which returns a function where the static argument is bound in its closure.

For example:

    var printAny[T](arg T ->) print(arg)
    var t1 = printAny type
    // t1 will be GenericFunctionType("T", (fn() T), (fn() Nothing))

    // this:
    var printInt = printAny[Int]
    // the [Int] part is a special instantiate expression which is its own
    // ast node since it's more special than a regular message

    var t2 = printInt type
    // t2 will be FunctionType(Int, Nothing)

What we need are static function literals. They are just like regular function literals, except that they are *evaluated* at check-time instead of being checked. Its runtime semantics are identical to a regular function:

    var a = staticfn(T) (fn(t T) print(t))
    var b = a[Int] // b = fn(t Int) print(t)
    b(123) // prints 123

The only difference is how the checker handles them. When a static function is checked, the checker does not evaluate the type of its body. (It can't: the body's type may have annotations which refer to the static parameters.) Instead, it stores the entire body as an expression, and the type of the static function literal becomes a special "StaticFunction" type that contains the static parameter names and the body.

    var a = staticfn(T) (fn(t T) print(t))
    // a's type is StaticFn("T", (fn() fn(t T) print(t) ))

The type-checker then handles instantiating a static function "specially". Given:

    var b = a[Int] // b = fn(t Int) print(t)
    
The type-checker will:

1. *Evaluate* the argument to instantiate (not evaluate it's type). So it 
   evaluates `Int` and gets the Int type object.
2. Create a static scope and bind the static type parameter name (`T`) to the
   evaluated argument `Int`.
3. Type-check the body in that scope so that type annotations within it can
   refer to the instantiated static arguments.
4. Return the type of the body.
