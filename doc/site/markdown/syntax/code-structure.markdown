^title Code Structure
^index 1

Before we get into the details of the different kinds of expressions in Magpie, there are a couple of rules that affect how Magpie code is formatted overall.

### Expressions

Unlike most imperative languages, but like most functional languages, Magpie does not have *statements*, only *expressions*. Flow control, blocks, and variable declarations are all expressions. This is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")

### Comments

Comments are as in C, C++, Java, etc.:

    :::magpie
    some code // this is a line comment
    // a line comment ends at the end of the line
    
    some more /* this is a block comment */ code code
    
    /* block comments
       can span multiple lines */

### Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // two expressions
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
wouldn't make sense. Specifically that means newlines following a comma (`,`),
colon (`:`), operator (`+`, `-`, etc.), or open brace (`(`, `[`, `{`) will be
discarded:

    :::magpie
    var a = 1,
            2 // a will be the tuple (1, 2)
    
    var b = 1 + 
            2 // b will be 3
    
    print(
        "hi") // prints "hi"

### Blocks

If you want to evaluate several expressions where only a single one is expected, you can create a *block*. Many languages use curly braces (`{ }`) for blocks. In Magpie, a block starts with a newline and ends with `end` (or occasionally another keyword like `else`):

    :::magpie
    if happy? then print("I'm happy!") // no block
    
    if happy? then // <- a newline here starts the block
        print("I'm happy!")
        print("Really happy!")
    end // <- and this ends it

Blocks are allowed most places where an expression is expected. In fact, blocks *are* expressions: they evaluate to the last expression in the block:

    :::magpie
    var a =
        print("hi")
        3
    end

This will print "hi" and then define `a` with the value 3.

A block creates a nested local scope. Variables declared inside disappear when
the block ends (unless they're captured in a closure, of course). For example:

    :::magpie
    var a =
        var temp = 1 + 2
        temp * temp
    end

After evaluating that, `a` will be 6 and `temp` will no longer exist.