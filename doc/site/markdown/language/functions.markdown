^title Functions
^index 4

Some purist OOP languages don't have functions at all&mdash; everything is objects and message sends. Magpie isn't quite so dogmatic. Its functions *are* objects, but they work more or less like you'd expect a normal function to behave.

### Creating Functions

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

Defining named functions is so command that there is a special keyword, `def` for doing just that. The above example would be more naturally expressed like:

    :::magpie
    def printArg(a) print("My arg is " + a)
    printArg("bananas!")

A function can also take a block for its body:

    :::magpie
    fn(i)
        i = i * 2
        print(i)
    end

If a function takes multiple arguments, they are separated by commas:

    :::magpie
    fn sum(a, b, c) a + b + c

(More precisely, the function still takes a single argument: a [tuple](compound-values.html). But, if you define a function with a list of comma-separated parameters like the above example, it will automatically pull the fields out of the tuple passed to the function and bind them to the listed names.)

If a function doesn't take any arguments, you can leave off the `()`:

    :::magpie
    fn print("hi")

### Calling Functions

Functions are called just like they were in your first algebra class: by putting the argument in parentheses after the function. Magpie has a built-in function called `print`. You can call it like:

    :::magpie
    print("this is the argument")

If a function doesn't take any arguments, it still needs to be explicitly invoked using `()`:

    :::magpie
    var sayHi = fn print("hi")
    sayHi   // doesn't call it, just gets a reference to the fn
    sayHi() // says hi

In Magpie all functions take a single argument. To pass multiple arguments, you actually create a [tuple](compound-values.html) of them, but the end result is that it looks like you'd expect:

    :::magpie
    sum(1, 2, 3)

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

    def makeFn(i)
        // return a function that references a variable
        // defined outside of itself (i)
        fn print(i)
    end
    
    var function = makeFn("hi")
    function() // prints "hi"

### Callables

Everything you've read so far assumes the thing you're calling is a function. What if it's not? What does this do:

    :::magpie
    "hello"(3)

If you try to apply an argument to an object that isn't a function, Magpie will translate that to a call to a method named `call` with the applied argument. So the above is equivalent to:

    :::magpie
    "hello" call(3)

Classes are free to implement `call` to do what they want. This lets you define your own objects that can be called like functions. String implements that get the character at the passed in index, so the above example returns "l".

In general, indexable collections like strings and arrays will implement this to handle getting items from the collection. Magpie doesn't have a `[]` syntax for accessing elements from arrays (square brackets are used for generics), so array access just looks like:

    someArray(2) // get the third element

