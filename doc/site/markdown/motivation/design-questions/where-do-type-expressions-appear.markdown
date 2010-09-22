^title Where Do Type Expressions Appear?

Answer:

1. After a param name in a function decl.
2. After `->` in a function decl.
3. In a field type decl.
4. In a cast expression.

These are different from just an expression evaluated at runtime on objects that happen to be types. These are cases where the type expression needs to be evaluated at type-check time because they affect the type-checking process.

### Q: Are there other cases where we want to define expressions that will be evaluated at type-check time?

A: *Yes:* type arguments to generic methods. They need to be evaluated at type-check time so that they can statically affect the parameter and return types of the method. Passing the type as a regular value parameter would work at runtime, but doesn't work at type-check time:

    var makeList(elementType IType -> List(???))

So, at a minimum, we'll need some syntax for passing type arguments to a method that is distinct from passing value arguments. One option:

    foo bar[typeArg](valueArg)

The type-checker will process that like:

    1. Evaluate `typeArg`.
    2. Check `valueArg`.
    3. Look up `bar` on `foo`'s type.
    4. Look up its type param name(s) and bind `typeArg` to it.
    5. Evaluate its param and return types in that context.
    6. Check that `valueArg`'s type matches the param type.
    7. Return the method's return type.

The interpreter will process that like:

    1. Evaluate `typeArg`.
    2. Evaluate `valueArg`.
    3. Look up `bar` on `foo`.
    4. Look up its type param name(s) and bind `typeArg` to it.
    5. Bind the argument to the param name(s).
    6. Evaluate the method body.
    7. Return the result.

Generic methods should cover constructing generic types:

    var people = List[Person] new
    var hash = Dictionary[String, Int] new

### Q: Are type arguments to generics erased?

A: *No.* Because we want to be able to access the type argument at runtime like
you can in C#, the type argument will have a runtime effect. For example:
   
    var makeList[E](-> List[E])
        print("making a list of " + E)
        /// ...
    end
   
### Q: Can you have "value template arguments"? I.e. things inside [] that don't evaluate to types?

A: *Sure*, there's no reason not to. The only real difference between stuff in
`[]` and stuff in `()` is that the former is evaluated at check time and the
latter is only checked. It would be clearer to call the former "static
arguments" and the latter "dynamic arguments".

### Q: We took brackets. What about arrays?

A: *Make them functors:*

    [1, 2, 3]    // becomes: Array of(1, 2, 3)
    array[1]     // becomes: array(1)
    array[1] = 2 // becomes: array(1) = 2