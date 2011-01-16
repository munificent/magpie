^title Blocks

To evaluate several expressions where only a single one is expected, you can create a *block*. Many languages use curly braces (`{ }`) for blocks. In Magpie, a block starts with a newline and ends with `end` (or occasionally another keyword like `else`):

    :::magpie
    if happy? then print("I'm happy!") // No block.

    if happy? then // <- A newline here starts the block.
        print("I'm happy!")
        print("Really happy!")
    end // <- And this ends it.

Blocks are allowed most places where an expression is expected. In fact, blocks *are* expressions: they evaluate to the last expression in the block:

    :::magpie
    var a =
        print("hi")
        3
    end

This will print "hi" and then define `a` with the value 3.

*TODO:* explain `catch`, `do`, and scoping.