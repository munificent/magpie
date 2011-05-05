^title Expressions

Expressions are the building blocks for programs. In Magpie, a programs is simply a list of them. Unlike most imperative languages, but like most functional
languages, Magpie does not have *statements*, only expressions. Flow control,
blocks, and variable declarations are all expressions which return values. This
is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")

The major types of expressions in Magpie are:

1. [Messages](expressions/messages.html)
2. [Calls](expressions/calls.html)
3. [Operators](expressions/operators.html)
4. [Variable declarations](expressions/variables.html)
5. [Blocks](expressions/blocks.html)
6. [Flow control](expressions/flow-control.html)

<p class="future">Need to add match, unsafecast, et. al.</p>