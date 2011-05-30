^title Primitives

Magpie is in its infancy, so it only has a small collection of primitive types built-in. Primitives can be created through *literals*, atomic expressions that evaluate to a value.

All primitive values in Magpie are *immutable*. That means that once created, they cannot be changed. `3` is always `3` and `"hi"` is always `"hi"`.

## Booleans

A boolean value represents truth or falsehood. There are two boolean literals, `true` and `false`. Its class is `Bool`.

## Numbers

Magpie doesn't have floating point numbers yet. (I know, I know. I'm working on it!) Integers look like you expect:

    :::magpie
    0
    1234
    -5678

Their class is `Int`.

<p class="future">
The long-term goal here is to have a pretty complete numeric tower &agrave; la Scheme or Python.
</p>

## Strings

String literals are surrounded in double quotes:

    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
    "\n" // Newline.
    "\"" // A double quote character.
    "\\" // A backslash.

Their class is `String`. Magpie strings are implemented internally using Java strings, so they are represented in UTF-16 format, although that shouldn't generally affect you. Most string operations in Magpie deal in logical characters, not bytes.

<p class="future">
Right now, getting a character out of a string returns another single-character string. Eventually, characters will be an atomic type in Magpie too.
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
