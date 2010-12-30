^title How are Generic Functions Checked?

Give a program like:

    var foo[A](arg A)
        arg bar
    end

    foo(Baz new)

How and when do we verify that:

1. `bar` is a valid message on `arg`.
2. `Baz` is a valid argument type.
3. That the generic parameter `A` can be inferred from `arg`?

## First Question

For the first question, there are basically two solutions:

1. C++-style. A generic method cannot be checked on its own. Instead, it gets
   instantiated at each callsite with specific concrete types, and its only then
   that it gets checked.
   
2. C# with constraints style. A generic method's type parameters are annotated
   with "where" clauses that limit the type of type arguments. A method can be
   checked against its constrained types.

C++ is a mess, so let's try to go with C# style. A type parameter like the above
one with no constraint would default to `Object` (or `Dynamic`?), meaning the
above code won't check because `Object` doesn't have a method `bar`. To fix it,
you'd have to do:

    var foo[A Baz](arg A)
        arg bar
    end

Where `Baz` is assumed to be some type that has a method `bar`. Type-checking a function now means:

1. Evaluate the type annotations of the static parameters.
2. In the function's type scope, bind those types to the static parameter names.
3. Evaluate the type annotations of the dynamic parameters in that scope.

That should conveniently alias `A` to `Baz`, so when we look up `A` later, we'll get the constrained type. Now we can type-check a method independent of its use. Win.

## Second Question

Now, the second question. How do we know that `Baz` is a valid (dynamic) argument type? Let's skip over inference now and just consider:

    foo[Baz](Baz new)

To check this, we just need to:
1. Evaluate the constraint on the static parameter (`Baz`).
2. Evaluate the static argument.
3. Test for compatibility.
4. In the function's type scope, bind the static argument value (not the
   constraint type) to the static parameter name (`A`).
5. Evaluate the type annotations of the dynamic parameters in that scope.

## Third Question

The last question, inferring static type arguments. That's going to get tricky. Consider a function like:

    var foo[A, B, C](a List[A], b B | C, c (A, (B, C)))

We need to answer two questions:
1. Are all static type parameters present in the dynamic parameter's type signature? (In this case, they are.)
2. If so, given an evaluated type for the dynamic argument and the expression tree for its parameter type, what are the values of the static type parameters?

The first one we can do statically independent of the actual type semantics by just walking the parameter type tree. The second one is hard because it's another core capability every type-like object will need to support. So the question is, given:

    var foo[A, B](a Dict[B, A])
    foo(Dict[String, Int] new)

Is there a way we can ask `Dict` to help us figure out what `A` and `B` are given `Dict[String, Int]`?

Here's one idea. We'll create a special tag type that just represents a placeholder for a type parameter, so that we can treat "A" and "B" as fake types. Given those, we can evaluate:

    Dict[B, A] // which desugars to Dict call[B, A]

And get a type object back (an instantiated `Dict`) with our special type tags embedded in it. Then we evaluate `Dict[String, Int]`, the actual argument type. Now we've got two objects we can line up, so we do:

    Dict[B, A] inferTypesFrom(typeMap, Dict[String, Int])

That will take some sort of map that maps parameter names like "A" to their inferred type. Every type will be expected to implement this. An implementation would look something like:

    def Dict[K, V] inferTypesFrom(typeMap, other IType)
        let dict = other as(Dict) then
            let keyType = K as(TypeParam) then
                typeMap map(keyType name, dict keyType)
            else
                K inferTypesFrom(typeMap, dict keyType)
            end
            let valueType = V as(TypeParam) then
                typeMap map(valueType name, dict valueType)
            else
                V inferTypesFrom(typeMap, dict keyType)
            end
        end
    end

Note the recursive calls to `inferTypesFrom`. Those handle nested types like
`Dict[(Int, String), List[String]]`. The `typeMap` will have to handle collisions where a type parameter appears more than once and is bound to conflicting types like:

    var foo[A](a A, b A)
    foo(1, true)

I think this would work. Handling or types and some other stuff may be a bit tricky. Figuring out how to reuse this code across all generic types will be a bit of work too.