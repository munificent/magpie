^title Functions
^index 4

A language can't call itself "functional" without having first-class functions: i.e. the ability to create functions and pass them around as values. To actually *be* functional, it also needs to have a nice syntax for doing so. Magpie uses the `fn` keyword for this. You can create a function object like this:

    :::magpie
    fn() print("I'm a fn!")

That creates an anonymous function that prints some text when invoked. You can invoke a function like you would in math class: by putting the argument to it in parentheses after it:

    :::magpie
    (fn(a) print("My arg is " + a)("bananas!")
    // prints "My arg is bananas!"

Because functions are first-class, you'd be more likely to put them in a variable and invoke them from there. The following example does the same as the above one:

    :::magpie
    var printArg = fn(a) print("My arg is " + a)
    printArg("bananas!")

A function can also take a block for its body:

    :::magpie
    fn(i)
        i = i * 2
        print(i)
    end

If a function doesn't take any arguments, you can leave off the `()`:

    :::magpie
    fn print("hi")

### Returning Values

A function automatically returns the value that its body evaluates to. An explicit `return` is not required:

    :::magpie
    fn 123 // returns 123 when called

If the body is a block, the result will be the last expression in the block:

    :::magpie
    fn
        print("hi")
        "result"
    end
    // prints "hi" then returns "result" when called

You can also explicitly return early from a function using a `return` expression:

    fn(arg)
        if arg == "no!" then return "bailed"
        "ok"
    end

If no expression follows the `return`, then it implicitly returns `nothing`.

### Closures

As you would expect, functions are
[closures](http://en.wikipedia.org/wiki/Closure_%28computer_science%29): they
can access variables defined outside of their scope. They will hold onto closed over variables even after leaving the scope where they are defined:

    var foo(i)
        // return a function that references a variable
        // defined outside of itself (i)
        fn print(i)
    end
    
    var f = foo("hi")
    f() // prints "hi"
