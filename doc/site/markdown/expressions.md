^title Expressions

Expressions are the building blocks for programs. In Magpie, a program is simply a series of them. Unlike most imperative languages, but like most functional languages, Magpie does not have *statements*, only expressions. Flow control, blocks, and variable declarations are all expressions which return values. This is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")
