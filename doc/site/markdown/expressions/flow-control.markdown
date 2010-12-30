^title Flow Control

Magpie doesn't shy away from imperative programming, so it supports the usual flow control structures you know and love, although it does try to spice them up a bit. Before we cover them, there's one minor point to cover first:

### Truthiness

Flow control is about evaluating an expression and then choosing an action based on whether or not the result is "true". That's easy if the value happens to be a boolean, but what if it's some other class?

Most languages handle this by having a set of rules for what values of any given type are "true" or not. Magpie calls this "truthiness". By default, the rules are:

* For booleans: `true` is truthy, and `false` is not, of course.
* Non-zero numbers are truthy.
* Non-empty strings are truthy.
* `nothing` is not truthy.
* All other objects are truthy.

However, this behavior isn't fixed. Truthiness is determined by sending a `true?` message to the object. The value returned by that is used to determine if a condition is met or not. So, if you define your own class, you can control how it behaves when used in a conditional expression by simply defining `true?` to do what you want. (To avoid the [turtles-all-the-way-down](http://en.wikipedia.org/wiki/Turtles_all_the_way_down) problem, the result from `true?` *does* have a fixed interpretation: a boolean `true` is the only truthy value.)

When reading the rest of this section, understand that any time a condition is evaluated, there is an implicit call to `true?` inserted. So this:

    :::magpie
    if something(other) then ...

Is really:

    :::magpie
    if something(other) true? then ...

### if/then/else

The simplest flow control structure, `if` lets you conditionally skip a chunk of code. It looks like this:

    :::magpie
    if ready then go!

That will evaluate the expression after `if`. If it's true, then the expression after `then` is evaluated. Otherwise it is skipped. The `then` expression can be a block:

    :::magpie
    if ready then
        getSet
        go!
    end

You may also provide an `else` expression. It will be evaluated if the condition is false:

    :::magpie
    if ready then go! else notReady

And, of course, it can take a block too. Note that if you have an `else` clause, and the `then` takes a block, you do *not* need an explicit `end` for the first block. The `else` will imply that:

    :::magpie
    if ready then
        getSet
        go!
    else
        notReady
    end

Since Magpie does not have statements, even flow control structures are expressions and return values. An `if` expression returns the value of the `then` expression if the condition was true, or the `else` expression if false. If there is no `else` block and the condition was false, it returns `nothing`.

    :::magpie
    print(if true then "yes" else "no") // Prints "yes".

(This also means that Magpie has no need for a C-style ternary operator. `if` *is* the ternary operator.)

### let/then/else

Magpie has another flow control structure similar to `if` called `let`. It lumps a couple of sequential actions together into a single operation:

1.  It evaluates an expression.
2.  If the result is not `nothing`, then it:
3.  Creates a new nested local scope.
4.  Binds the result to a variable in that scope.
5.  Then evaluates an expression in that scope.

That probably sounds kind of arbitrary. An example will help. The `Int` class has a static method for parsing strings to numbers, like so:

    :::magpie
    var i = Int parse("123") // i will be 123.

If the parse fails (i.e. the string isn't a valid number), then it returns `nothing`. Normally, you'd need to check this manually, like so:

    :::magpie
    var maybeNumber = Int parse(someString)
    if maybeNumber != nothing then
        // Do something with the number...
    end

A `let` expression simplifies that:

    :::magpie
    let definitelyNumber = Int parse(someString) then
        // Do something with the number...
    end

The nice thing about this is that if that variable exists at all, we know for certain that it will not be `nothing`. If it was, `let` would have skipped it entirely. Think of it as a variable that is scoped to within the success of an operation.

This is a handy convenience when we're just considering Magpie as a dynamic language. When we take into account its static typing, it gets more useful:

    :::magpie
    var maybeNumber = Int parse(someString)
    if maybeNumber != nothing then
        // Here, maybeNumber's type is Int | Nothing.
        // That means that we can't do...
        maybeNumber abs
        // ...because "abs" isn't a method on Nothing.
        // We would have to explicitly cast first.
    end

    let definitelyNumber = Int parse(someString) then
        // But here, definitelyNumber's type is just Int.
        // The type-checker knows it cannot be Nothing,
        // so this is fine:
        definitelyNumber abs
    end

There is another bit of syntactic sugar `let` provides. If you leave off the conditional expression, it will infer it to be a variable with the same as the one being defined. This lets you take an existing variable that may be `nothing` and create a new shadowed one that will definitely not be:

    :::magpie
    var num = Int parse(someString)
    let num then
        // In here we've created a new variable "num" with
        // the same value as the outer one, but the type-
        // checker knows its type cannot be Nothing.
        num abs
    end

<p class="future">
Need to document pattern-matching syntax here.
</p>

### Loops

Many languages have three distinct loop structures: `for`, `while`, and `foreach` or something similar for generators. Magpie only has a single loop construct, but it subsumes all three of those. A simple loop looks like this:

    :::magpie
    while something do
        body
    end

The `while something` part is the *clause*, and the expression after `do` (here a block) is the *body*. The clause is evaluated at the beginning of each loop iteration (including before the first). If it fails, the loop body is skipped and the loop ends. There are two kinds of clauses:

#### while

The simplest is `while`. It evaluates a condition expression. If it evaluates to false, the loop ends, otherwise it continues. For example:

    while 1 < 2 do print("forever...")

#### for

The other loop clause is `for`. It looks like:

    for item = collection do print(item)

The expression after `for` is evaluated *once* before the loop starts. The result of that should be an object that implements the `Iterable` interface. `Iterable` has one method, `iterate`, that is expected to return an object that implements `Iterator`.

An iterator generates a series of values. It has two members, a `next()` method and a `current` getter. `next()` will be called before each loop iteration (including the first) and advances the iterator to its next position. If the iterator is out of values, it returns `false` and the clause fails, otherwise it returns `true`. `current` returns the current value that the iterator is sitting on.

Each iteration of the loop, Magpie will advance the iterator and bind the current value to a variable. The variable is scoped within the body of the loop. What this means is that a loop like:

    :::magpie
    for item = collection do print(item)

Is really just syntactic sugar for:

    :::magpie
    var __iter = collection iterate
    while __iter next do
        var item = __iter current
        print(item)
    end

All of this means that Magpie *only* has a `foreach`-like for loop. It doesn't have a primitive C-style `for` loop. To get the classic "iterate through a range of numbers" behavior, the standard library provides methods on numbers that return iterators:

    :::magpie
    for i = 1 to(5) do print(i)
    // Prints "1", "2", "3", "4", "5".

    for i = 1 until(5) do print(1)
    // Prints "1", "2", "3", "4".

Here, `to` and `until` are just regular methods and not built-in keywords. They each return iterators that will iterate through a series of numbers.

#### Multiple Clauses

I said earlier that Magpie only has a single loop expression. That's because clauses can actually be combined. A single loop can have as many clauses as you want, of either type, in any order. At the beginning of each iteration of a loop (including before the first one), *all* of the clauses are evaluated in the order they appear. If *any* clause fails, the loop body is skipped and the loop ends. For example:

    :::magpie
    while happy
    while knowIt do
        clap(hands)
    end

Once one of those `while` clauses returns false, the loop ends. With `for` loops, the iterators are iterated in *parallel*, unlike nested loops:

    :::magpie
    for i = 1 to(3)
    for j = 6 to(10) do
        print(i + ":" + j)
    end
    // Prints "1:6", "2:7", "3:8".

    for i = 1 to(4) do
        for j = 6 to(10) do
            print(i + ":" + j)
        end
    end
    // Prints "1:6", "1:7", "1:8", "1:9", "1:10", "2:7" ... "4:10".

The key difference is `do`. That's the keyword that indicates the end of the
clauses and the beginning of the body. No matter how many clauses you have, a
single `do` means a single loop.

Allowing multiple `for` clauses like this enables some handy idioms:

    :::magpie
    // Iterate through a collection with indexes
    for i = 0 countingUp
    for item = collection do
        // Here 'item' is the item and 'i' is its index.
    end

    // Iterate through the first N items in a collection.
    for i = 1 to(n)
    for item = collection do print(item)

    // Iterate through a collection until an item is found.
    var found = nothing
    while found == nothing
    for item = collection do
        if test(item) then found = item
    end

#### Exiting Early

Inside the body of a loop, you can exit early using a `break` expression:

    :::magpie
    while true do
        something
        if badNews then break
        somethingElse
    end

#### Return Value

Because loops are expressions, they return a value.

<p class="future">
Right now, a loop will always return `nothing`, but that will likely change. It will be possible to define an `else` clause for a loop and then the expression will return the last result of evaluating the body, the expression passed to `break` or the expression after `else` if the body was never entered.
</p>

### Conjunctions

What other languages call "logical operators", Magpie calls "conjunctions". They are considered flow control expressions and not operators because they conditionally execute some code&mdash; they short-circuit. There are two conjunctions in Magpie: `and` and `or`. Both of them are infix operators, like so:

    :::magpie
    happy and knowIt
    ready or not

An `and` conjunction evaluates the left-hand argument. If it's not true, it returns that value. Otherwise it evaluates and returns the right-hand argument. An `or` conjunction is reversed. If the left-hand argument *is* true, it's returned, otherwise the right-hand argument is evaluated and returned:

    print(0 and 1) // prints 0
    print(1 and 2) // prints 2
    print(0 or 1)  // prints 1
    print(1 or 2)  // prints 1

Note that logical negation is *not* a built-in flow control expression. Instead, `not` is simply a method on booleans:

    if happy not then print("sad")

<p class="future">
"xor" doesn't really need to be a conjunction since it doesn't short circuit, but it (and maybe "not") likely will be at some point just to be consistent.
</p>