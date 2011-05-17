^title Calls

Most of the [expressions](expressions.html) you'll write in Magpie will be *calls*. A call invokes a [multimethod](multimethods.html) by name, passing in the provided argument. Method calls may have one of three different "fixities" in Magpie, which means the name of the method may appear before, after, or in the middle of the argument.

It's important to note that the fixity distinction is purely syntactic. Semantically, all method calls are just calls that take a single argument. Magpie is flexible here is so that you can define methods that read most clearly when called.

## Prefix Calls

A prefix method call is a method name followed by an argument. The argument must be in parentheses.

    :::magpie
    print("Hello!")

This uses `nothing` as the implicit left-hand argument, so the above is identical to:

    :::magpie
    nothing print("Hello!")

Prefix calls are most useful for operations where no argument is "special" and where the operation doesn't feel like an intrinsic property of the argument. The above *could* be `"Hello!" print` but that would feel a bit strange since printing doesn't seem like an intrinsic capability of strings.

## Infix Calls

An infix method call is a left-hand expression followed by the method name, followed by a right-hand argument. Like prefix calls, the right-hand argument must be in parentheses (but the left does not).

    :::magpie
    list add("item")

This is a single call that invokes the `add` method. The arguments are `list` and `"item"`. (More accurately, the argument is a single [record](records.html) `(list, "item")`, but the method definition syntax mostly spares you from worrying about it like that).

If you're wondering where the `.` went, the answer is that Magpie doesn't use one between the left-hand argument and the method. In general, Magpie eschews punctuation when possible and this is an example of that. Instead, `.` is just another character that can be used in identifiers.

## Postfix Calls

An expression followed by a name defines a postfix method call.

    :::magpie
    "a string" count

This invokes the `count` method with `"a string"` as the lone argument. Method calls like this are called *getters*. While they look different, they are still just regular method calls.

Note that in this case, the argument is just `"a string"` while a prefix call like `count("a string")` would have the argument `(nothing, "a string")`. Prepending `nothing` for postfix calls makes it possible for a multimethod to distinguish between a prefix and postfix method with the same name.

A call expression may *not* omit *both* the left- and right-hand arguments. A name by itself is a [name expression](variables.html). This lets Magpie statically (i.e. just from parsing) tell if a name is used to look up a variable or to call a method.

## Indexers

The named call syntax we've seen covers most code you'll read and write in Magpie, but it also offers a little special sugar for accessing collection-like objects. An *indexer* is a method call that uses square brackets.

    :::magpie
    collection[3]

Here, we're calling an indexer method. The left-hand argument is `collection` and the right-hand is `3`. While they look different, indexers are just another notation for method calls. You can define your own indexers and they can be overloaded just like other multimethods. If `collection` here was an instance of an `Array` class, the indexer method called above could be defined like this:

    :::magpie
    def (this is Array)[index is Int]
        // Get item at index...
    end

Indexers aren't limited to numeric arguments inside the square brackets.

    :::magpie
    def (this is Grid)[x is Int, y is Int]
        // Get item at position...
    end

Here we've defined an indexer for a `Grid` class that accesses at point on a two-dimensional space. It can be called like this:

    :::magpie
    grid[2, 3]

## Implicit Receivers and `this`

Many OOP languages like C++ and Java allow omitting the receiver in a method call. If omitted, it will be inferred to be `this`. In Magpie, there is no implicit receiver since methods aren't directly tied to classes. Instead, you have to manually specify the object you want to be the left-hand argument, even if it is the same object that was passed as the left-hand to the current method.

    :::magpie
    def (this is String) doubleCount
        this count * 2
    end

Here we're calling the `count` method and using the same left-hand argument as was passed to `doubleCount`. We still have to manually specify `this`. A reference to `count` by itself will be interpreted as a variable reference.

The corollary to this is that `this` isn't anything special in Magpie. It's conventional to use it for the left-hand argument, but convention is all it is. From the language's perspective, `this` is just another variable.

## Chaining Calls

Call expressions are left-associative, which follows how method calls work in other object-oriented languages. This way, a series of method calls can be read from left to right.

    :::magpie
    addressBook names find("Waldo") email()

This expression will be parsed like:

    :::magpie
    ((addressBook names) find("Waldo")) email()

**TODO: `with` block arguments**
