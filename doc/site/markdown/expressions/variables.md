^title Variables

Variables are named slots for storing values.

## Defining Variables

You can define a new variable in Magpie using the `var` keyword, like so:

    :::magpie
    var a = 1 + 2

This creates a new variable `a` in the current scope and initializes it with the
result of the expression following the `=`. Like everything, a variable definition is an expression in Magpie. It evaluates to the initialized value:

    :::magpie
    print(var a = "hi") // prints "hi"

## Accessing Variables

Once a variable has been defined, it can be accessed by name as you would expect. It is an error to attempt to access a variable whose name doesn't exist.

<p class="future">Currently, this is a runtime error. Eventually, it will be a static error reported before the module begins executing.</p>

## Patterns

In our previous examples, the part between `var` and the `=` has always been a simple name, but what you're seeing there is actually a [pattern](../patterns.html). That means any pattern is allowed in a declaration. A simple case is what other languages call "multiple assignment":

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

## Scope

Variables in Magpie have true block scope: they exist from the point they are defined until the end of the [block](blocks.html) where that definition appears.

    :::magpie
    do
        print(a) // "nothing"
        var a = 123
        print(a) // "123"
    end
    print(a) // "nothing"

All variables are lexically scoped. There is no top level global scope in Magpie. Each module has its own top-level scope that isn't shared with other modules.

## Shadowing

Declaring a variable in an inner scope with the same name as an outer one is called *shadowing* and is not an error (although it's not something you likely intend to do much):

    :::magpie
    var a = "outer"
    if true then
        var a = "inner"
        print(a) // Prints "inner".
    end
    print(a) // Prints "outer".

Declaring a variable with the same name in the *same* scope *is* an error:

    :::magpie
    var a = "hi"
    var a = "again" // Error!

<p class="future">Currently, the second line will throw a <code>RedefinitionError</code> at runtime. In the future, this will be a statically detected error that will be reported before any code in the file is executed.</p>

## Assignment

After a variable has been declared, you can assign to it using `=`:

    var a = 123
    a = 234

An assignment will walk up the scope stack to find where the named variable is declared. If it can't find a variable with that name, it will generate a runtime error.

Like variable definition, an assignment expression returns the assigned value:

    var a = "before"
    print(a = "after") // prints "after"

<p class="future">
TODO: document setter methods here.
</p>
