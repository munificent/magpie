^title Compound Values
^index 3

If the previous section covered [atoms](atomic-values.html), then this section covers molecules: values that are built by combining other values. The most important compound values in Magpie are instances of classes, which are covered in their [own section](classes.html), but there are also a couple of built-in compound types.

### Tuples

A series of expressions separated by commas creates a *tuple*:

    :::magpie
    1, 2, "three"  // Creates a tuple of two ints and a string.

If you aren't familiar with tuples, it may be a bit hard to wrap your head around them. A tuple is a *compound* value, but it isn't quite a *container*. A tuple doesn't *hold* values, it *is* values.

The simplest example is a point. A point in 2D space doesn't *hold* an X and a Y coordinate, it *is* one. You can't have a tuple that contains just one value&mdash; a single value is already a tuple (a mono-uple?) in the same way that a 1-dimensional point is just a number.

Tuples are a core part of Magpie. When you call a message with multiple arguments, you're actually passing a single one: a tuple.

#### Accessing Fields

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

### Records

A record is similar to a tuple but where the fields are identified by name instead of number. Another way to look at them is as anonymous structures. The
syntax is:

    :::magpie
    x: 1 y: 2
    // Creates a record with fields "x" and "y"
    // Whose values are 1 and 2, respectively.

Note that no separators are needed between the fields. The field names (followed by a colon) are enough to distinguish them.

While records look like dictionaries or maps in some languages, they are have one important difference: they are strongly-typed. The set of fields and their types is part of the *type* of a record. A record `x: 1 y: 1` has a different static type than `x: 1 y: 1 z: 1` or even `x: 1 y: "a string"`. (If you're familiar with any of the ML languages, this is familiar territory.)

#### Accessing Fields

Fields in a record can be accessed like any other field on an object, by name:

    :::magpie
    var point = x: 1 y: 2
    print(point x) // Prints "1".
    print(point y) // Prints "2".

Records are immutable, so fields cannot be assigned to.

### Expression Objects

An expression object is a chunk of code that isn't evaluated. Instead, it's just
bundled up into a data structure that you can pass around. You can't do much
with them yet, but they will eventually be used for metaprogramming. To create
an expression literal (as opposed to creating an expression that's evaluated
in-place), just enclose any expression in curly braces:

    :::magpie
    var a = { print("hi") }

That will create an expression object containing the code `print("hi")` and store a reference to it in `a`. It won't print anything.

Expression objects are similar to [functions](functions.html)&mdash; they both contain a chunk of code. The main difference is that a function carries with it enough context (i.e. its closure and its parameters) so that it can be invoked. An expression literal strips that out: it's just a piece of code stored as data. You can't directly evaluate an expression object.

<p class="future">
The syntax for these may change depending on how useful they turn out to be. There may be more valuable things to use {} for if metaprogramming is something rarely used in practice.
</p>