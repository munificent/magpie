^title Primitives

Primitives are the built-in object types that all other objects are composed from. They can be created through *literals*, atomic expressions that evaluate to a value.

All primitive values in Magpie are *immutable*. That means that once created, they cannot be changed. `3` is always `3` and `"hi"` is always `"hi"`.

## Booleans

A boolean value represents truth or falsehood. There are two boolean literals, `true` and `false`. Its class is `Bool`.

## Numbers

Magpie has two numeric types, integers and floating-point. Number literals look like you expect:

    :::magpie
    // Ints
    0
    1234
    -5678

    // Floats
    3.14159
    1.0
    -12.34

Integers and floating-point numbers are instances of different classes (`Int` and `Float` respectively) and there are no implicit conversions in Magpie. This means that if you have a [method](multimethods.html) that expects a `Float`, then passing `1` won't work. You'll need to pass `1.0`.

In practice, this is rarely an issue. Most arithmetic operations have specializations for both kinds of numbers and will work fine regardless of what you pass. In cases where mixed argument types are passed, then the result will be a float.

If you want to write code that works generically with either kind of number, then you want the `Num` class. Both `Int` and `Float` inherit from that, so a method specialized to `Num` will accept either type.

## Strings

String literals are surrounded in double quotes:

    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
    "\n" // Newline.
    "\"" // A double quote character.
    "\\" // A backslash.

Their class is `String`.

## Characters

Characters are instance of the `Char` class and represent the individual Unicode code points that make up strings. When you index into a string or iterate over it, character objects will be returned. They also have a literal form, which is the character surrounded by single quotes:

    :::magpie
    'A'
    '!'

<p class="future">
There will also be escape sequences for characters, but they haven't been implemented yet.
</p>

## Nothing

Magpie has a special primitive value `nothing`, which is the only value of the
class `Nothing`. (Note the difference in case.) It functions a bit like `void`
in some languages: it indicates the absence of a value. An `if` expression with
no `else` block whose condition is `false` evaluates to `nothing`. Likewise, a
method like `print()` that doesn't return anything actually returns `nothing`.

It's also similar to `null` in some ways, but it doesn't have [the
problems](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-
nothing/) that `null` has in most other languages. It's rare that you'll
actually need to write `nothing` in code since it can usually be inferred from
context but it's there if you need it.

Since `nothing` is in its own class, that means that a method that is [specialized](multimethods.html) to another class won't receive `nothing` instead. In Magpie, you never need to do this:

    :::magpie
    def length(string is String)
        // Make sure we got a string.
        if string == nothing then return 0

        string count
    end

If your method expects a string, you'll get a string, not a string-or-maybe-nothing.
