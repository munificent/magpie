^title Quotations

Metaprogramming is writing code that operates on objects that themselves
represent code. In other words: programs that write programs. Think of it as
software navel-gazing. Like any powerful tool, metaprogramming can be used to
obscure, mystify, and cause harm. But in the hands of the benevolent, it can be
a useful and expressive feature.

Every language supports metaprogramming to some degree or another. You can write
a C program that manipulates another C program as a string if you really want
to. The question is how easy it is to do.

Magpie tries to make it easier by defining classes in the standard library for
representing chunks of Magpie code&mdash; *expression objects*&mdash; and
because it has *quotations*: a concise syntax for creating them.

## Expression Classes

In the Magpie base library are a set of expression classes. Each class
represents a different kind of built-in expression in the Magpie syntax. For
example, an instance of `VarExpression` represents a variable declaration
expression. It has fields for the name of the variable and the expression used
to initialize it. You can create an instance of it like you would any other
object:

    :::magpie
    var expression = VarExpression new("age", IntExpression new(32))

This will create an object that represents the chunk of code `var age = 32`.
Since everything in Magpie is an expression, you can use these classes to
construct objects that represent entire Magpie programs. You can pull apart,
compose and otherwise play with code using these, just like any other data
structure.

<p class="future">
The full set of expression classes will be documented with the standard lib at
some point.
</p>

## Expression Literals

By itself though, this isn't really usable. This chunk of code:

    :::magpie
    VarExpression new("age", IntExpression new(32))

doesn't look very much like the code it represents:

    :::magpie
    var age = 32

To remedy that, Magpie supports *quotations*. A quotation looks like a chunk of
code surrounded by curly braces. When evaluated, instead of evaluating that code
directly, it just creates an expression object that represents the code. It's a
literal notation for expression object. In other words, these two lines of code
are equivalent:

    :::magpie
    var expr = VarExpression new("age", IntExpression new(32))
    var expr = { var age = 32 }

After creating a quotation, you can treat the result like any regular object. For example, a `VarExpression` has a `name` getter to get the name of the field being defined. You can access it like this:

    :::magpie
    var expr = { var someVariable = "the value" }
    print(expr name) // prints "someVariable"

## Unquoting

Quotations let us build expressions in a readable just like string literals let us create strings more easily than concatenating a series of characters together. But that still only covers the simple case of creating a fixed expression object from scratch. What if we want to compose an expression object or insert pieces of other expressions into one? Say we have:

    :::magpie
    var expr = { if zombie then print("Brains!") }

But instead of always initializing having the body of the `if` expression be
`print("Brains!")`, we want to be able to provide an expression that will be
inserted into there? To address that, quotations support *unquoting*. Unquoting
is very similar to string interpolation. The basic idea is that within a
quotation, you can indicate an expression that should be *evaluated* right then
instead of just treated like a chunk of code. For example:

    :::magpie
    var action = { shamble(around) }
    var expr = { if zombie then `action }

The backtick (<code>\`</code>) character there tells Magpie that the following
expression should be evaluated in place. In other words, the value of `expr`
will be an object that represents the chunk of code:

    :::magpie
    print(if zombie then shamble(around))

The value of the `action` variable&mdash;itself an expression object&mdash; has
been inserted into the quotation.

### Unquote Syntax

There are a couple of ways you can unquote. The simplest one we have already
seen: you can follow the backtick immediately with a name:

    :::magpie
    { `someVariable }

If you want to evaluate a more complex expression after the backtick, you can
use parentheses:

    :::magpie
    { `(call some method(passing, args)) }

### Unquoting Primitive Values

It's important to note that result of an unquote must be an expression object.
Attempting to do this is an error:

    :::magpie
    var foo = Foo new() // make some random object
    var expr = { print(`foo) }

Since `Foo` presumably isn't a valid expression class, this won't work. One
common case where occurs is with primitive values. Let's say you want to create
a function that will return an expression print a given person's name and age. You might try:

    :::magpie
    def makeExpression(name String, age Int -> Expression)
        { print(`name ~~ "is" ~~ `age ~~ "years old.")
    end

But there's a problem. We're unquoting `name` and `age`, but those aren't expressions, they're just primitive values. What we actually intend to do is insert *literal expressions* for those values, like so:

    :::magpie
    def makeExpression(name String, age Int -> Expression)
        var nameExpr = StringExpression new(name)
        var ageExpr  = IntExpression new(age)
        { print(`nameExpr ~~ "is" ~~ `ageExpr ~~ "years old.")
    end

This is correct, but tedious and unnecessarily verbose. To help here, Magpie will automatically convert a primitive value to a literal expression of the
appropriate type when it's used in an unquote. So our first `makeExpression` example will work and do what you expect.

<p class="future"> The syntax for quotations (i.e. using curlies) may
change. Metaprogramming isn't something that users are likely to do often
(thankfully!), so it may make sense to free up {} for something more useful like
array or map literals and use something a bit longer for quotations.
</p>
