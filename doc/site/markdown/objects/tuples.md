^title Tuples

A series of expressions separated by commas creates a *tuple*:

    :::magpie
    1, 2, "three"  // Creates a tuple of two ints and a string.

If you aren't familiar with tuples, it may be a bit hard to wrap your head around them. A tuple is a *compound* value, but it isn't quite a *container*. A tuple doesn't *hold* values, it *is* values.

The simplest example is a point. A point in 2D space doesn't *hold* an X and a Y coordinate, it *is* one. You can't have a tuple that contains just one value&mdash; a single value is already a tuple (a mono-uple?) in the same way that a 1-dimensional point is just a number.

Tuples are a core part of Magpie. When you call a message with multiple arguments, you're actually passing a single one: a tuple.

## Accessing Fields

Once you have a tuple, fields can be pulled out by sending it a message whose name is `_` followed by the zero-based index of the field:

    :::magpie
    var a = "one", 2
    print(a _0) // prints "one"
    print(a _1) // prints "2"

Tuple fields are also implicitly decomposed when passed to a function with multiple named arguments. For example:

    :::magpie
    // define a function that takes two values
    def takeTwo(a, b)
        print(a + " + " b)
    end

    // make a tuple
    var tuple = "peanut butter", "jelly"

    // pass it to the function
    takeTwo(tuple) // prints "peanut butter + jelly"
