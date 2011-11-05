^title Flow Control

**TODO: This needs to be re-organized since this is now in the core library and not the language.**

Magpie doesn't shy away from imperative programming, so it supports the usual flow control structures you know and love, although it does try to spice them up a bit. Before we cover them, there's one minor point to cover first:

## Truthiness

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

## if/then/else

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

## Conjunctions

What other languages call "logical operators", Magpie calls "conjunctions". They are considered flow control expressions and not operators because they conditionally execute some code&mdash; they short-circuit. There are two conjunctions in Magpie: `and` and `or`. Both of them are infix operators, like so:

    :::magpie
    happy and knowIt
    ready or not

An `and` conjunction evaluates the left-hand argument. If it's not true, it returns that value. Otherwise it evaluates and returns the right-hand argument. An `or` conjunction is reversed. If the left-hand argument *is* true, it's returned, otherwise the right-hand argument is evaluated and returned:

    :::magpie
    print(0 and 1) // prints 0
    print(1 and 2) // prints 2
    print(0 or 1)  // prints 1
    print(1 or 2)  // prints 1

Note that logical negation is *not* a built-in flow control expression. Instead, `not` is simply a method on booleans:

    :::magpie
    if happy not then print("sad")

<p class="future">
"xor" doesn't really need to be a conjunction since it doesn't short circuit, but it (and maybe "not") likely will be at some point just to be consistent.
</p>