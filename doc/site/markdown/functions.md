^title Functions

When you want to bundle up a reusable chunk of code in Magpie, you'll usually use a [method](multimethods.html). But sometimes you want a chunk of code that you can pass around like a value. For that, you'll use a *function*. Functions are first-class objects that encapsulate an executable expression.

## Creating Functions

Functions are defined using the `fn` keyword followed by the expression that forms the body of the function.

    :::magpie
    fn print("I'm a fn!")

This creates an anonymous function that prints `"I'm a fn!"` when called. The body of a function can be a single expression like above or can be a [block](blocks.html).

    :::magpie
    fn
        print("First!")
        print("Second!")
    end

## Parameters

To make a function that takes an argument, put a [pattern](patterns.html) for it in parentheses after the `fn` keyword.

    :::magpie
    fn(name, age) print("Hi, " + name + ". You are " + age + " years old.")

Like with methods, any kind of pattern can be used here. Go crazy.

### Implicit Parameters

When programming in a functional style, you often have lots of little functions that just call a method or do some trivial expression. Here's a line of code to pull the even numbers from a collection:

    :::magpie
    val evens = [1, 2, 3, 4, 5] where(fn(n) n % 2 == 0)

To make this a little more terse, Magpie supports *implicit parameters*. The above code can also be written:

    :::magpie
    val evens = [1, 2, 3, 4, 5] where(fn _ % 2 == 0)

Note that the parameter pattern is gone, and `n` in the body has been replaced with `_`.

The rule for implicit parameters is pretty simple. If a function has no parameter pattern, then a pattern will be created for it. Every `_` that appears in the body of the function will be replaced with a unique variable for each occurrence. Then a pattern will be created that defines those variables in the order that they appear.

The "unique variable" and "order that they appear" parts are important here, since you can have multiple implicit parameters. When you do, each `_` becomes its *own* parameter for the function.

    :::magpie
    fn (_ + _) / _

This creates a function with *three* separate implicit parameters. It's equivalent to:

    :::magpie
    fn(a, b, c) (a + b) / c

Implicit parameters can help code be more readable when the function body is small and the parameters are obvious from the surrounding context. But they can also render your code virtually unreadable (like the above example here) otherwise. Like all pointy instruments, wield it with care.

## Calling Functions

Once you have a function, you call it by invoking the `call` method on it. The left-hand argument is the function, and the right-hand argument is the argument passed to the function.

    :::magpie
    var greeter = fn(who) print("Hi, " + who)
    greeter call("Fred") // Hi, Fred

If a function doesn't take an argument, then there won't be a right-hand argument to `call`.

    :::magpie
    var sayHi = fn print("Hi!")
    sayHi call

Like methods, the argument pattern for a function may include tests. If the argument passed to `call` doesn't match the function's pattern, it throws a `NoMethodError`.

    :::magpie
    var expectInt = fn(n is Int) n * 2
    expectInt call(123) // OK
    expectInt call("not int") // Throws NoMethodError.

If you pass too many arguments to a function, the extra ones will be ignored.

    :::magpie
    var takeOne = fn(n) print(n)
    takeOne("first", "second") // Prints "first".

However, if you pass too *few*, it will throw a `NoMethodError`.

    :::magpie
    var takeTwo = fn(a, b) print(a + b)
    takeOne("first") // Throws NoMethodError.

## Returning Values

A function automatically returns the value that its body evaluates to. An explicit `return` is not required:

    :::magpie
    var name = fn "Fred"
    print(name call) // Fred

If the body is a block, the result is the last expression in the block:

    :::magpie
    var sayHi = fn
        print("hi")
        "result"
    end
    sayHi call // Prints "hi" then returns "result".

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

    :::magpie
    var counter = makeCounter()
    print(counter call // Prints "1".
    print(counter call) // Prints "2".
    print(counter call) // Prints "3".

## Callables

The `call` method used to invoke functions is a regular multimethod with a built-in specialization for functions. This means you can define your own "callable" types, and specialize `call` to act on those. With that, you can use your own callable type where a function is expected and it will work seamlessly.
