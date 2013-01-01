^title Variables

Variables are named slots for storing values. You can define a new variable in Magpie using the `var` keyword, like so:

    :::magpie
    var a = 1 + 2

This creates a new variable `a` in the current scope and initializes it with the result of the expression following the `=`. Like everything, a variable definition is an expression in Magpie. It evaluates to the initialized value.

    :::magpie
    print(var a = "hi") // prints "hi"

Once a variable has been defined, it can be accessed by name as you would expect.

    :::magpie
    var name = "Abraham Lincoln"
    print(name) // prints "Abraham Lincoln"

## Patterns

In our previous examples, the part between `var` and the `=` has always been a simple name, but what you're seeing there is actually a [pattern](patterns.html). That means any pattern is allowed in a declaration. A simple case is what other languages call "multiple assignment".

    :::magpie
    var x, y = 1, 2
    print(x + ", " + y) // 1, 2

Here, the record pattern `x, y` is used to destructure the record `1, 2`, pulling out the fields into separate variables. More complex nested records also work.

    :::magpie
    var coloredPoint = (position: (2, 3), color: "red")
    var position: (x, y), color: c = coloredPoint
    print(x + ", " + y + " " + c) // 2, 3 red

Here we're destructuring a nested record in the same way. Patterns that test values are supported as well.

    :::magpie
    var good is String = "a string"
    var bad is String = 123

The first line of this will execute without a problem, but the second line will throw a `NoMatchError` because `123` is not a string.

(One way to think of variable definitions is as [match](pattern-matching.html) expressions with one case whose body is the rest of the current scope.)

## Scope

A variable in Magpie has true block scope: it exists from the point where it is defined until the end of the [block](blocks.html) where that definition appears.

    :::magpie
    do
        print(a) // ERROR! a doesn't exist yet
        var a = 123
        print(a) // "123"
    end
    print(a) // ERROR! a doesn't exist anymore

All variables are lexically scoped. There is no single global scope in Magpie. Instead, each [module](modules.html) has its own top-level scope that isn't shared with other modules.

Declaring a variable in an inner scope with the same name as an outer one is called *shadowing* and is not an error (although it's not something you likely intend to do much).

    :::magpie
    var a = "outer"
    if true then
        var a = "inner"
        print(a) // Prints "inner".
    end
    print(a) // Prints "outer".

Declaring a variable with the same name in the *same* scope *is* an error.

    :::magpie
    var a = "hi"
    var a = "again" // ERROR!

## Assignment

After a variable has been declared, you can assign to it using `=`.

    :::magpie
    var a = 123
    a = 234

An assignment will walk up the scope stack to find where the named variable is declared. It's an error to assign to a variable that isn't defined. Magpie doesn't roll with implicit variable definition.

Like variable definition, an assignment expression returns the assigned value.

    :::magpie
    var a = "before"
    print(a = "after") // Prints "after".

You can use can also use record pattern syntax to destructure when assigning to existing variables.

    :::magpie
    var a, b = 1, 2
    a, b = 3, 4
    print(a + ", " + b) // Prints "3, 4".

## Single-Assignment Variables

The variables we've seen so far all allow assignment. You can prevent that by defining them using `val` instead of `var`.

    :::magpie
    val a = "before"
    a = "after" // ERROR!

Single-assignment variables can make code easier to understand since you don't have to hunt around and see if they ever gets reassigned. Arguments to [methods](multimethods.html), variables bound in [pattern match expressions](pattern-matching.html), and [loop iterators](looping.html) are always defined as single-assignment.
