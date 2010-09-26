^title Literals
^index 2

Literals are the atomic building blocks of a language. Magpie currently doesn't have a very wide set of them, but it's getting there. It supports:

### Booleans

A boolean value (type `Bool`) can be `true` or `false`.

### Numbers

Magpie doesn't have floating point numbers yet. (I know, I know. I'm working on it!) Integers look like you expect:
    
    :::magpie
    0
    1234
    -5678

### Strings

Strings are surrounded in double quotes:
    
    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
    "\n" // newline
    "\"" // a quote
    "\\" // a backslash

### Nothing

Magpie has a special type `Nothing` that has only one value `nothing`. (Note the difference in case.) It functions a bit like `void` in some languages: it indicates the absence of a value. A function like `print` that doesn't return anything actually returns `nothing`.

It's also similar to `null` in some ways, but it doesn't have [the
problems](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-nothing/)
that `null` has in most other languages. It's rare that you'll actually need to
write `nothing` in code. Most of the time the interpreter will infer it or let
you omit it, but it's there if you need it.
