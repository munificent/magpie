^title Blocks

In any imperative language, you often want to perform a series of actions in sequence. You usually do this by creating a *block*. A block contains a series of expressions. When the block is evaluated, each expression is evaluated in turn. The result of the last expression is the value of the block. The values returned by previous expressions are discarded.

## Syntax

Magpie has a peculiarly lightweight syntax for defining blocks. Consider a simple [if](flow-control.html) expression. If we just want to evaluate a single expression when the condition is met, we can do so like this:

    :::magpie
    if isHappy then print("I'm happy!")

If we want to evaluate a series of expressions when the condition is met, we need to create a block:

    :::magpie
    if isHappy then
        print("I'm happy!")
        print("Really happy!")
    end

A block begins with a newline where an expression is expected. The parser knows that an expression must follow a `then`. If it sees a newline there instead, it knows that a block is being provided. It will then parse a series of expressions separated by newlines until it reaches the terminator for the block.

Normally, the terminator is the `end` keyword, but it may be another keyword based on the context in which the block occurs. For example, you can terminate the `then` block in an `if` expression with `else`:

    :::magpie
    if isHappy then
        print("I'm happy!")
        print("Really happy!")
    else // Terminates the then block.
        print("I'm not happy")
    end

This all sounds a bit fishy, but in practice it works well with a minimum of punctuation. There is one important bit to keep in mind with this: you cannot have an expression on the first line if you want to create a block:

    :::magpie
    if isHappy then print("I'm happy!") // ERROR
        print("Really happy!")
    end

If you do so, it will assume that the first expression is all that is expected, and subsequent lines will not be part of the block.

## Do Blocks

If you want to create a block in the middle of another block, you can create a `do` block:

    :::magpie
    print("one")
    do
        print("two")
        print("three")
    end
    print("four")

This can be useful if you want to limit the scope of a [variable](variables.html) or [catch](error-handling.html#catching-errors) a thrown error.
