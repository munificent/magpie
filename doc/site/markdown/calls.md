^title Calls

Most of the [expressions](expressions.html) you'll write in Magpie will be *calls*. A call invokes a [multimethod](multimethods.html) by name, passing in the provided argument. Method calls may have one of three different "fixities" in Magpie, which means the name of the method may appear before, after, or in the middle of the argument.

It's important to note that the fixity distinction is purely syntactic. Semantically, all method calls are just calls that take arguments. Magpie is flexible here is so to let you define APIs that read naturally when called.

## Prefix Calls

A *prefix* method call is a method name followed by an argument. The argument must be in parentheses.

    :::magpie
    print("Hello!")

Prefix calls are most useful for operations where no argument is "special" and where the operation doesn't feel like an intrinsic property of the argument. The above *could* be `"Hello!" print` but that would feel a bit strange since printing doesn't seem like an intrinsic capability of strings.

## Infix Calls

An *infix* method call is a left-hand expression followed by the method name, followed by a right-hand argument. Like prefix calls, the right-hand argument must be in parentheses (but the left does not).

    :::magpie
    list add("item")

This is a single call that invokes the `add` method. The arguments are `list` and `"item"`.

If you're wondering where the `.` went, the answer is that Magpie doesn't use one between the left-hand argument and the method. In general, Magpie eschews punctuation when possible and this is an example of that. Instead, `.` is just another character that can be used in identifiers.

## Postfix Calls

An expression followed by a name defines a *postfix* method call.

    :::magpie
    "a string" count

This invokes the `count` method with `"a string"` as the lone argument.

**TODO: Infix and prefix operator calls.**

## Subscript Operators

The named call syntax we've seen covers most code you'll read and write in Magpie, but it also offers a little special sugar for accessing collection-like objects. A *subscript operator* is a method call that uses square brackets.

    :::magpie
    collection[3]

Here, we're calling a subscript operator method. The left-hand argument is `collection` and the right-hand is `3`. While they look different, subscript operators are just another notation for method calls. You can define your own subscripts and they can be overloaded just like other multimethods. If `collection` here was an instance of an `List` class, the indexer method called above could be defined like this:

    :::magpie
    def (this is List)[index is Int]
        // Get item at index...
    end

Subscript operators aren't limited to numeric arguments inside the square brackets. Any [pattern](patterns.html), including record ones is valid.

    :::magpie
    def (this is Grid)[x is Int, y is Int]
        // Get item at position...
    end

Here we've defined a subscript for a `Grid` class that accesses a point on a two-dimensional space. It can be called like this:

    :::magpie
    grid[2, 3]

## Chaining Calls

Call expressions are left-associative, which follows how method calls work in other object-oriented languages. This way, a series of method calls can be read from left to right.

    :::magpie
    addressBook names find("Waldo") sendEmail

This expression will be parsed like:

    :::magpie
    ((addressBook names) find("Waldo")) sendEmail

**TODO: `with` block arguments**
