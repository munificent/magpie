^title Welcome

Thanks for coming!

Magpie is a small dynamically-typed programming language built around [*patterns*](patterns.html), [*classes*](classes.html), and [*multimethods*](multimethods.html). From functional languages, it borrows first-class [functions](functions.html), closures, expressions-for-everything, and [quotations](quotations.html). Its most novel feature is probably an [extensible syntax](syntax-extensions.html). It runs on the [JVM](http://en.wikipedia.org/wiki/Java_Virtual_Machine).

It looks a bit like this:

    :::magpie
    // Generates the sequence of turns needed to draw a dragon curve.
    // See: http://en.wikipedia.org/wiki/Dragon_curve
    def dragon(0, _)
        ""
    end

    def dragon(n is Int, turn)
        dragon(n - 1, "R") + turn + dragon(n - 1, "L")
    end

    print(dragon(5, ""))

Its goal is to let you write code that's beautiful and easy to read, and to allow you to seamlessly extend the language and libraries as you see fit.

## Where We're At

Here's the deal. Magpie is very young. The egg has been laid, but still hasn't
quite cracked open yet. Everything described here is implemented and working
now, but lots of stuff is missing, buggy, or likely to change. I wouldn't
entrust it with my lunch money if I were you.

If you just want a language to *use*, this is bad news. But if you want a
language to *contribute to*, this is great news! Lots of interesting important
decisions have yet to be made, and there's lots of fun stuff that needs coding.
I'd love to have you involved.

## Getting Started

It should be pretty easy to get it up and running. You'll need to:

1. **Pull down the code.** It lives here: <tt><a href="https://github.com/munificent/magpie">https://github.com/munificent/magpie</a></tt>

2. **Build it.** The repo includes an Eclipse project if that's your thing. If
   you rock the command-line, you can just do:

        $ cd magpie
        $ ant jar

3. **Run it.** Magpie is a command line app. After building the jar, you can
   run it by doing:

        $ ./magpie

   If you run it with no arguments, it drops you into a simple
   [REPL](http://en.wikipedia.org/wiki/REPL). Enter a Magpie expression and it
   will immediately evaluate it. Since *everything* is an expression, even things like class definitions, you can build entire programs incrementally this way. Here's one to get you started:

        :::magpie
        for i = 1 to(20) do print("<your name> is awesome!")

   If you pass an argument to the app, it will assume it's a path to a script
   file and it will load and execute it:

        $ ./magpie script/Hello.mag

## Where to Go From Here

Since you've made it this far, you must be interested. You can learn more about
the language from the [Magpie posts](http://journal.stuffwithstuff.com/category/magpie/) on my blog. (Note that the language has changed dramatically over time, so the older a post is, the less likely it is to still be relevant.)

To get a sense of the language itself, take a look at some [examples](https://github.com/munificent/magpie/tree/master/example), the [standard library](https://github.com/munificent/magpie/tree/master/lib) or executable language [specification](https://github.com/munificent/magpie/tree/master/spec), written in Magpie.

If you have questions or comments, the mailing list
[here](http://groups.google.com/group/magpie-lang) is a good place to start. You can [file bugs or issues on github](https://github.com/munificent/magpie/issues). If you want some live interaction, there's an IRC channel on freenode: [#magpie-lang](irc://irc.freenode.net/magpie-lang).

Cheers, and have fun playing with it!
