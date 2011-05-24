^title Pattern Matching

Control flow&mdash; deciding which code to execute&mdash; is a big part of imperative languages. To execute a chunk of code more than one time, you use [looping](looping.html). To conditionally *skip* a chunk of code, you need some kind of *branching*. Magpie only has one branching construct built-in, but it's pretty swell: *pattern-matching*.

A `match` expression evaluates a *value* expression. Then it looks at each of a series of *cases*. For each case, it tries to match the case's [*pattern*](patterns.html) against the value. If the pattern matches, then it evaluates the *body* of the case.

    :::magpie
    val fruit = "lemon"
    match fruit
        case "apple" then print("apple pie")
        case "lemon" then print("lemon tart")
        case "peach" then print("peach cobbler")
    end

The expression after `match` (here just `fruit`) is the value being matched. Each line starting with `case` until the final `end` is reached defines a potential branch the control flow can take. When a match expression is evaluated, it tries each case from top to bottom. The first case here doesn't match because `"apple"` isn't `"lemon"`, so its body is skipped. The second case *does* match. That means we execute its body, print `"lemon tart"` and we're done. Once a case has matched, the remaining cases are skipped.

Like everything in Magpie, `match` expressions are *expressions*, not statements, so they return a value: the result of evaluating the body of the matched case. That means we can reformulate the above example like so:

    :::magpie
    val fruit = "lemon"
    val dessert = match fruit
        case "apple" then "apple pie"
        case "lemon" then "lemon tart"
        case "peach" then "peach cobbler"
    end
    print(dessert)

Or even:

    :::magpie
    val fruit = "lemon"
    print(match fruit
        case "apple" then "apple pie"
        case "lemon" then "lemon tart"
        case "peach" then "peach cobbler"
    end)

A case body may also be a block, as you'd expect. If it's the last case in the match, the block must end with `end`, otherwise, the following `case` is enough to terminate it:

    :::magpie
    match dessert
        case "apple pie" then
            print("apple")
            print("pie crust")
            print("ice cream")
        case "lemon tart" // "case" here ends "apple pie" block
            print("lemon")
            print("pastry shell")
        end // last case block must end with "end"
    end // ends entire "match" expression

## Case Patterns

With simple literal patterns, this doesn't look like much more than `switch` statements in other languages, but Magpie allows you to use any pattern as a case. With that, you can bind variables, destructure objects, or branch based on type:

    :::magpie
    def describe(obj)
        match obj
            case b is Bool          then "Bool : " + b
            case n is Int           then "Int : " + n
            case s is String        then "String : " + s
            case x is Int, y is Int then "Point " + x + ", " + y
        end
    end

    describe(true) // "Bool : true"
    describe(123)  // "Int : 123"
    describe(3, 4) // "Point : 3, 4"

If the pattern for a case binds a variable (like `b` in the first case here) that variable's scope is limited to the body of that case. That way, we can ensure that you'll only get a variable bound if it matches what you want. For example, here we know for certain that `b` will only exist if `obj` is a boolean and `b` will be its value.

## Match Failure

It's possible for no case in a match expression to match the value. If that happens, it will [throw](error-handling.html) a `NoMatchError`. This is the right thing to do if you only expect certain values and a failure to match is a programmatic error. If you *do* want to handle any possible value, though, you can add an `else` case to the match expression:

    :::magpie
    val dessert = match fruit
        case "apple" then "apple pie"
        case "lemon" then "lemon tart"
        case "peach" then "peach cobbler"
        else "unknown fruit"
    end

If no other pattern matches, the `else` case will.
