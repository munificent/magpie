^title Language
^index 2

This section covers the core Magpie language: its syntax, built-in types, and expressions. My hope is that you'll find it to be a good bit simpler than many other languages. Syntactically, its main inspirations are [Io](http://www.iolanguage.com/) (messages for everything, minimal punctuation) and [Ruby](http://www.ruby-lang.org/en/) (keywords and blocks). If you find those tolerably readable, Magpie shouldn't bother you too much.

To give you a preview, here's a (thoroughly useless) chunk of code that contains just about every piece of syntax:

    :::magpie
    var everything = fn()
        for i = 1 to(5)
        while i * i < 10 do
            print(i toString(base: 10 pad: 6)
            if i < 3 then break
            let a = Int parse("1" + i) then
                return { an expression }
            else if i < 3 then break
        end
    end

Magpie's semantics follow in the footsteps of Smalltalk: dynamic types and
dispatch, everything is an object, work is done by sending messages, objects are
members of classes, etc. This is broken down into:

1. [Program Structure](language/program-structure.html)
1. [Atomic Values](language/atomic-values.html)
1. [Compound Values](language/compound-values.html)
1. [Functions](language/functions.html)
1. [Messages](language/messages.html)
1. [Variables](language/variables.html)
1. [Flow Control](language/flow-control.html)
1. [Classes](language/classes.html)