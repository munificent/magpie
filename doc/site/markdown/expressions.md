^title Expressions

Expressions are the building blocks for programs. In fact, programs *are*
expressions. Unlike most imperative languages, but like most functional
languages, Magpie does not have *statements*, only expressions. Flow control,
blocks, and variable declarations are all expressions which return values. This
is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")

Coming from another imperative language, you should find that Magpie has fewer distinct kinds of expressions, but that each one is used for more different things. For example, instead of a distinct array element syntax like `array[index]`, Magpie just uses function calls: `array(index)`. Likewise, Magpie unifies `do`, `while`, `for`, and iterator loops under a single looping construct.

The major types of expressions in Magpie are:

1. [Messages](expressions/messages.html)
2. [Calls](expressions/calls.html)
3. [Operators](expressions/operators.html)
4. [Variable declarations](expressions/variables.html)
5. [Blocks](expressions/blocks.html)
6. [Flow control](expressions/flow-control.html)

<p class="future">Need to add match, unsafecast, et. al.</p>