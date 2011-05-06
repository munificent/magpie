^title Functions

When you want to bundle up a reusable chunk of code in Magpie, you'll usually use a [method](multimethods.html). But sometimes you want a chunk of code that you can pass around like a value. For that, you'll use a *functions*. Functions are first-class objects that encapsulate an executable chunk of code.

## Creating Functions

Functions are defined using the `fn` keyword followed by the expression that forms the body of the function.

    :::magpie
    fn print("I'm a fn!")

This creates an anonymous function that prints `"I'm a fn!"` when called. If a function takes an argument, the [pattern](patterns.html) for it is placed in parentheses after the keyword.

    :::magpie
    fn(name, age) print("Hi, " + name + ". You are " + age + " years old.")

The body of a function can be a single expression as you've seen, but can also be a block.

    :::magpie
    fn(i)
        i = i * 2
        print(i)
    end

## Calling Functions

Once you have a function, you can call it by invoking the `call` method on it. The left-hand argument is the function, and the right-hand argument is the argument passed to the function.

    :::magpie
    var greeter = fn(who) print("Hi, " + who)
    greeter call("Fred") // Hi, Fred

If a function doesn't take an argument, you should invoke `call` with `()`.

    :::magpie
    var sayHi = fn print("Hi!")
    sayHi call()

Like methods, the argument pattern for a function may include tests. If the argument passed to `call` doesn't match the function's pattern, it throws a `NoMethodError`.

    :::magpie
    var expectInt = fn(n is Int) n * 2
    expectInt call(123) // OK
    expectInt call("not int") // Throws NoMethodError.

## Returning Values

**TODO(bob): This applies to methods too. Move there.**

A function automatically returns the value that its body evaluates to. An explicit `return` is not required:

    :::magpie
    var name = fn "Fred"
    print(name call()) // Fred

If the body is a block, the result is the last expression in the block:

    :::magpie
    var sayHi = fn
        print("hi")
        "result"
    end
    sayHi call() // Prints "hi" then returns "result".

If you want to return before reaching the end of the function body, you can use an explicit `return` expression.

    :::magpie
    var earlyReturn = fn(arg)
        if arg == "no!" then return "bailed"
        print("got here")
        "ok"
    end

This will return `"bailed"` and print nothing if the argument is `"no!"`. With any other argument, it will print `"got here"` and then return `"ok"`.

A `return` expression with no expression following the keyword (in other words, a `return` on its own line) implicitly returns `nothing`.

## Closures

As you would expect, functions are
[closures](http://en.wikipedia.org/wiki/Closure_%28computer_science%29): they
can access variables defined outside of their scope. They will hold onto closed-over variables even after leaving the scope where the function is defined:

    :::magpie
    def makeCounter()
        var i = 0
        fn i = i + 1
    end

Here, the `makeCounter` method returns the function created on its second line. That function references a variable `i` declared outside of the function. Even after the function is returned from `makeCounter`, it is still able to access `i`.

    var counter = makeCounter()
    print(counter call()) // Prints 1.
    print(counter call()) // Prints 2.
    print(counter call()) // Prints 3.

## Callables

The `call` method used to invoke functions is a regular multimethod with a built-in specialization for functions. This means you can define your own "callable" types, and overload `call` to act on those. With that, you can use your own callable type where a function is expected and it will work seamlessly.
