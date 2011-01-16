^title Expressions

Expressions are the building blocks for programs. In fact, programs *are*
expressions. Unlike most imperative languages, but like most functional
languages, Magpie does not have *statements*, only expressions. Flow control,
blocks, and variable declarations are all expressions which return values. This
is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")

Magpie has the usual expressions you know and love from most imperative languages: [blocks](expressions/blocks.html), [message sends](expressions/messages.html), [function calls](expressions/calls.html), [operators](expressions/operators.html), [variables](expressions/variables.html), and [flow control](expressions/flow-control.html).