^title Expression Objects
^index 7

Metaprogramming is writing code that operates on objects that themselves
represent code. In other words: programs that write programs. Think of it as
software navel-gazing. Like any powerful tool, metaprogramming can be used to
obscure, mystify, and cause harm. But in the hands of the benevolent, it can be
a useful and expressive feature.

Every language supports metaprogramming to some degree or another. You can write
a C program that manipulates another C program as a string if you really want to. The question is how easy it is to do.

Magpie tries to make it easier by defining expressions classes in the standard library for representing chunks of Magpie code, and because it has *expression literals*: a concise syntax for creating instances of those classes.

### Expression Classes

In the Magpie base library are a set of expression classes. Each class represents a different kind of built-in expression in the Magpie syntax. For example, an instance of `VarExpression` represents a variable declaration expression. It has fields for the name of the variable and the expression used to initialize it. You can create an instance of it like you would any other object:

    :::magpie
    var expression = VarExpression new("age", IntExpression new(32))

This will create an object that represents the chunk of code `var age = 32`.
Since everything in Magpie is an expression, you can use these classes to 
construct objects that represent entire Magpie programs. You can pull apart, compose and otherwise play with code using these, just like any other data structure.

<p class="future">
The full set of expression classes will be documented with the standard lib at
some point.
</p>

### Expression Literals

By itself though, this isn't really usable. This chunk of code:

    :::magpie
    VarExpression new("age", IntExpression new(32))

doesn't look very much like the code it represents:

    :::magpie
    var age = 32

To fix that, Magpie supports *expression literals*. An expression literal is a chunk of code surrounded by curly braces. When evaluated, instead of evaluating that code directly, it just creates an expression object that represents the code. In other words, these two lines of code are equivalent:

    :::magpie
    var expr = VarExpression new("age", IntExpression new(32))
    var expr = { var age = 32 }

After evaluating an expression literal, you can treat the result like any regular object. For example:

    :::magpie
    var expr = { var someVariable = "the value" }
    print(expr name) // prints "someVariable"

### Quasiquoting

This gets us a lot farther. Now we've got a pretty simple syntax to build chunks
of fixed code, but what if the code objects we're building have parts that vary?
What if you want to do:

    :::magpie
    var expr = { var age = 32 }

But instead of always initializing `age` to `32`, you want to be able to vary
that value? To address that, expression literals support *quasiquoting*. Quasiquoting is very similar to string interpolation. The basic idea is that within a syntax literal, you can indicate an expression that should be *evaluated* right then instead of just treated like a chunk of code. For example:

    :::magpie
    var age = { 23 }
    var expr = { print("You are " ~ `age ~ " years old!") }

The backtick (<code>\`</code>) character there tells Magpie that the following expression should be evaluated in place. In other words, the value of `expr` will be an object that represents the chunk of code:

    :::magpie
    print("You are " ~ 23 ~ " years old!")

The value of the `age` variable has been inserted into our expression literal.

It's important to note that the value of `age` was itself an expression&mdash; `{ 23 }`&mdash; and not just the raw number `23`. That's because we're inserting an *expression* into our expression literal, not a *value*. The *number* 23 isn't a valid expression object in Magpie (though it is a valid expression, and a valid object!). If you want an expression representing the *literal* 23, you need an instance of `IntExpression`, which is what `{ 23 }` will evaluate to.

It's a bit confusing, I'll admit. The short summary is that when you use quasiquoting, the values you insert will always need to be an instance of one of the expression types.

### Quasiquote Expressions

There are a couple of ways you can quasiquote inside an expression. The simplest one we have already seen: you can follow the backtick immediately with a name:

    :::magpie
    { `someVariable }

If you want to evaluate a more complex expression after the backtick, you can use parentheses:

    :::magpie
    { `(call some method(passing, args)) }

Since the result of a quasiquote must be an expression, you can also use curly braces immediately after the backtick to immediately wrap the result in one:

    :::magpie
    var age = 23 // note: not an expression
    var expr = { print("You are " ~ `{ age } ~ " years old!") }

<p class="future">
The syntax for expression literals (i.e. using curlies) may change. Metaprogramming isn't something that users are likely to do often (thankfully!), so it may make sense to free up {} for something more useful like array or map literals and use something a bit longer for expresion literals.
</p>
