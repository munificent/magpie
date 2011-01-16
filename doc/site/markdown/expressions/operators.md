^title Operators

Like Smalltalk and Lisp, but unlike most other languages, Magpie does not have any built-in operators. Any name that starts with a punctuation character defines an *operator*. These are all valid operators:

    :::magpie
    +   -   /   *#$%   +something    *3

The only difference between an operator and a regular identifier is that operators appear in infix position&mdash; the two arguments to them surround the operator.

<p class="future">
This is out-of-date. Operators don't work this way any more!
</p>

## Using Operators

Operators are used just like you'd expect:

    :::magpie
    1 + 2 // Evaluates to three.

Under the hood, that gets translated to calling the `+` method on `1`, passing in `2` as the argument. This means that operators aren't totally symmetric: the left-hand argument becomes the receiver, so it has greater control over how the operator is handled.

Because Magpie doesn't have a fixed set of operators, it also doesn't have a complex precedence or associativity table for them either. Instead, all operators have the same precedence and associate left to right. In other words, this in Magpie:

    :::magpie
    1 + 2 * 3

evaluates to *9* and not *7*. Parentheses are your friend here. This may take some getting used to, but I think the simplicity (no complicated operator precedence to remember) and flexibility (define your own operators for DSLs) are worth it.

## Unary Operators

All operators in Magpie are *binary*, they have an argument on either side of them. Most other languages have a couple of unary operators too, like `!` for complement and `-` for negation. In Magpie, those are just regular named [messages](messages.html):

    :::magpie
    // Magpie  // Other languages
    value neg  // -value
    truth not  // !truth
