^title Compound Literals
^index 3

The previous section covered *atomic* literals in Magpie: the smallest building blocks that Magpie data is built out of. Magpie also has a few kinds of *compound literals*. These build values out of smaller pieces. (Technically, function expressions could be considered literals too, but they're important enough to warrant their own section.)

### Tuples

A series of expressions separated by commas creates a *tuple*:

    :::magpie
    1, 2, "three"  // creates a tuple of two ints and a string

If you aren't familiar with tuples, it may be a bit hard to wrap your head around them. A tuple is a *compound* value, but it isn't quite a *container*. A tuple doesn't *hold* values, it *is* values.

The simplest example is a point. A point in 2D space doesn't *hold* an X and a Y coordinate, it *is* one. You can't have a tuple that contains just one value&mdash; a single value is already a tuple (a mono-uple?) in the same way that a 1-dimensional point is just a number.

Tuples are a core part of Magpie. When you call a message with multiple arguments, you're actually passing a single one: a tuple.

#### Accessing Fields

Once you have a tuple, fields can be pulled out by sending it a message whose name is `_` followed by the zero-based index of the field:

    :::magpie
    var a = "one", 2
    print(a _0) // prints "one"
    print(a _1) // prints "2"

(Tuple fields are also implicitly decomposed when passed to a function with multiple named arguments.)

### Object Literals

An object literal builds a new object from scratch. Its class will be `Object`, and it will have the given fields defined on it. The syntax is:

    :::magpie
    var point = x: 1 y: 1
    // creates an object with fields "x" and "y"

Note that no separators are needed between the fields. The field names (followed by a colon) are enough to distinguish them.

#### Accessing Fields

The fields in an object literal can be accessed like any other field on an object, by their name:

    :::magpie
    var point = x: 1 y: 2
    print(point x) // prints "1"
    print(point y) // prints "2"

### Expression Literals

An expression literal is a chunk of code that isn't evaluated. Instead, it's just bundled up into a data structure that you can pass around. You can't do much with them yet, but they will eventually be used for metaprogramming. To create an expression literal, just enclose any expression in curly braces:

    :::magpie
    var a = { print("hi") }

That will create an expression object containing the code `print("hi")` and store a reference to it in `a`. It won't print anything.

Expression literals are similar to functions: they both contain a chunk of code. The main difference is that a function carries with it enough context (i.e. its closure and its parameters) so that it can be invoked. An expression literal strips that out: it's just a piece of code stored as data.