^title Operators

Like Smalltalk and Lisp, but unlike most other languages, Magpie does not have any built-in operators. Any name that contains *only* punctuation characters is considered an *operator*. These are all valid operators:

    :::magpie
    +   -   /   *#$%    *3

The only difference between an operator and a regular [message](messages.html) is that operators appear in infix position&mdash; the two arguments to them surround the operator.

## Using Operators

Operators are used just like you'd expect:

    :::magpie
    1 + 2 // Evaluates to 3.

Under the hood, the parser will simplify that to a regular function [call](calls.html). The above example will be spit out by the parser like:

    :::magpie
    +(1, 2)

This means that defining your own operators is just defining a function that takes two arguments. If we wanted to create an operator that repeats a given string a certain number of times, we could define it like this:

    :::magpie
    def **(string String, count Int -> String)
        var result = ""
        for i = 1 to(count) do result = result ~ string
        result
    end
    
    var thrice = "beep" ** 3 // "beepbeepbeep"

## Overloading Operators

An operator is just a function bound to a variable whose happens to be punctuation. This means there's no real way to overload operators. If you define an operator with the same name as an existing one, it will either replace or shadow it.

<p class="future">This may change if I can get multimethods working.</p>

To get around this limitation, the common operators defined in the base library that you would need to be able to overload, such as equality (`==`) work by calling a method on one or both of their operands. In order to allow your class to be used with `==`, you just need to add a shared `equal?` method on your class that compares the two instances, and the `==` operator will call that when the operands are of your class's type.

## Precedence and Associativity

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
