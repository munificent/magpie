^title Syntax
^index 2

Magpie's syntax is a good bit simpler than many other languages. Its main inspirations are [Io](http://www.iolanguage.com/) (messages for everything, minimal punctuation) and [Ruby](http://www.ruby-lang.org/en/) (keywords and blocks). If you find those tolerably readable, Magpie shouldn't bother you too much.

To give you a preview, here's a (thoroughly useless) chunk of code that contains just about every piece of Magpie syntax:

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

The following sections break all of that down in detail:

1. [Code Structure](syntax/code-structure.html)
1. [Literals](syntax/literals.html)
1. [Compound Literals](syntax/compound-literals.html)
1. [Functions](syntax/functions.html)
1. [Messages](syntax/messages.html)
1. [Variables](syntax/variables.html)
1. [Flow Control](syntax/flow-control.html)
1. [Precedence](syntax/precedence.html)
