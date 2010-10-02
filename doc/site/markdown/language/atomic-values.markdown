^title Atomic Values
^index 2

*Atomic values* are the smallest built-in kinds of objects in the language. They
are represented in code using *literals*. All atomic values in Magpie are
*immutable*: once created, their value cannot be changed. This includes strings.

Magpie currently doesn't have a very wide range of atomic values, but it's
getting there. It supports:

### Booleans

A boolean value can be `true` or `false`. Its class is `Bool`.

### Numbers

Magpie doesn't have floating point numbers yet. (I know, I know. I'm working on it!) Integers look like you expect:
    
    :::magpie
    0
    1234
    -5678

Their class is `Int`.

<p class="future">
The eventual goal is for the runtime behavior of numbers to be arbitrary precision "correct" numbers ala Scheme or Python. Meanwhile, the type system will have a stricter set of numeric types so that you can annotate when you expect something to be integral or floating point.
</p>

### Strings

Strings are surrounded in double quotes:
    
    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
    "\n" // newline
    "\"" // a quote
    "\\" // a backslash

Their class is `String`. Magpie strings are implemented internally using Java strings, so they are represented in UTF-16 format, although that shouldn't generally affect you. Most string operations in Magpie deal in logical characters, not bytes.

<p class="future">
Right now, getting a character out of a string returns another single-character string. Eventually, character will be an atomic type in Magpie too.
</p>

### Nothing

Magpie has a special value `nothing`, which is the only value of the class
`Nothing`. (Note the difference in case.) It functions a bit like `void` in some
languages: it indicates the absence of a value. An `if` expression with no
`else` block whose condition is `false` evaluates to `nothing`. Likewise, a
function like `print` that doesn't return anything actually returns `nothing`.

It's also similar to `null` in some ways, but it doesn't have [the
problems](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-nothing/)
that `null` has in most other languages. It's rare that you'll actually need to
write `nothing` in code. Most of the time the interpreter will infer it or let
you omit it, but it's there if you need it.
