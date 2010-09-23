^title Welcome

Thanks for coming. Let's get started. Magpie is an object-oriented programming language for the JVM that combines the safety of dynamic typing with the flexibility of static typing. Maybe I have that backwards. Let's hope so.

It looks a bit like this:

    // generates the sequence of turns needed to draw a dragon curve
    // see: http://en.wikipedia.org/wiki/Dragon_curve
    var dragon(n, turn)
        if n > 0 then
            dragon(n - 1, "R") + turn + dragon(n - 1, "L")
        else ""
    end

    print(dragon(5))

### Why Another Language?

There are a slew of languages out there already. What's so interesting about this one? Here's the stuff that gets me excited about it:

*   **It's dynamic.** It duck types and lets you pass around objects freely. 
    It's a nice loose language for prototyping and tinkering. It doesn't have
    the boilerplate and ceremony of straightjacket enterprise languages.   
    Here's a complete program in Magpie:
    
        print("Hi!")

*   **It type checks.** On the other hand, sometimes it's really nice to be able
    to say, "Anything you pass to this method is gonna need to support X, Y, and 
    Z" and not have to write exhaustive unit tests to ensure that. So Magpie 
    lets you optionally provide type annotations to methods. If you do, it will 
    *statically* check that you're doing the right thing before ever calling 
    `main()`.
    
    The goal is to give you all of the safety of a static language, but only
    where you actually want it.
    
    The type system is pretty neat too.
    [Tuples](http://journal.stuffwithstuff.com/2009/05/05/one-and-only-one/) and
    union types are built-in and, unlike most languages, `null` is [not a valid
    value](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-nothing/)
    of all types: no more [checking for `null`
    everywhere](http://lambda-the-ultimate.org/node/3186). It has interfaces
    which duck type like
    [Go](http://golang.org/doc/effective_go.html#interfaces_and_types).
    Generics, intersection types, and structural types are on the to do list.

*   **It's an open language.** Classes and objects are freely open to extension 
    in Magpie. You should never need to create abominations like:
    
        MyStringUtils.wishIWasAMethodOnString(someString)
    
    Just add it to the `String` class. Unlike other highly flexible languages,
    Magpie tries to make this safe and manageable by providing other features
    like static checking to make sure you don't accidentally break things.

*   **It reads easy and writes flexibly.** Magpie's core syntax is based on
    simple message sends with a minimum of punctuation. By example:
    
        var items = Array new
        items add("zero")
        for i = 1 to(10) do items = items ++ i toString
        print(items)
        
    There are no predefined operators in Magpie: any sequence of punctuation
    characters is valid. (Punctuation characters are also fine in regular
    identifiers too). Operator precedence is flat as in Smalltalk. Sometime 
    soon, you should even be able to define your own custom keywords and control
    structures (although the default ones are pretty nice too).

*   **It's functional.** Closures, higher-order functions, and the other fun 
    toys you like from functional languages are available in Magpie.
    
*   **It has an extensible type system.** All types (the "things" that the 
    static type-checker uses) are first-class objects in Magpie. Most of 
    Magpie's type system is implemented itself in Magpie. (And it type-checks
    itself.) This gives you the flexibility to extend the type system, and it
    means that type annotations are just regular Magpie expressions evaluated
    during type-checking.

*   **It runs on your OS.** The Magpie interpreter is written in Java and runs 
    on the JVM, so wherever you go, Magpie can probably go with you. At some
    point soon, it should be able to talk directly to existing Java libraries.
    One intended use case is as a scripting language for JVM apps.
    
### No, Really, Why Another Language?

That bullet list covers the points, but I don't think it captures the essence.
There's a certain *way* that I want to be able to program, and Magpie is my
attempt to implement it. The heart of it is that a program runs in two stages.

The first stage is where the contructs of the program are being assembled together. This is where you're creating classes, defining methods, wiring things up, injecting dependencies and generally getting it all to hang together.

I want that to be as open and flexible as possible. I should be able to add methods to the classes where it makes sense for them to be. I should be able to define new interfaces and then have existing classes I didn't write implement them. I should be able to use a config file to swap out how a logging class is implemented or turn off debug-only features, and I should be able to procedurally generate a class from a SQL schema on a live DB.

But but but! Once that's done, I want some guarantee that I didn't screw up. After all of the tents have been raised and the sawdust put down, but before the lights go on, I want the language to automatically go through everything and tell me if I'm doing things correctly and consistently. I don't care when and where I added a method to some class, but before my program starts running, I want the compiler to yell at me if I didn't name it right.

If that sounds like the way you'd like to code, Magpie might be the language for you.

### Now the Bad Part

But here's the deal. It's still very early days for Magpie. It isn't
vaporware&mdash; everything described here is implemented and working now. But
it's still really early and lots of stuff is missing, buggy, or likely to
change. I wouldn't put my bank account in it if I were you.

If you just want a language to use, this is bad news. But if you want to work on a language, this is great news! Lot of interesting important decisions have yet to be made, and there's lots of fun stuff that needs coding. I'd love to have you involved.

### Getting Started

It should be pretty easy to get it up and running. You'll need to:

1. **Pull down the code.** It lives here: [http://bitbucket.org/munificent/magpie](http://bitbucket.org/munificent/magpie)

2. **Build it.** I'm using Eclipse right now (I'm not a Java expert yet), so you
   should be able to just open it up and run it. If there's another build
   process you'd prefer, let me know.
   
3. **Run it.** Magpie is a command line app. If you run it with no arguments,
   it drops you into a primitive [REPL](http://en.wikipedia.org/wiki/REPL). 
   Enter Magpie a expression (type <tt>Enter</tt> twice to complete it) and it
   will immediately evaluate it. Since everything is an expression, you can even
   build classes this way.
   
   If you run it with a single argument "test", it will run the test suite.
   Otherwise, it will assume the argument is a path to a script file and it will
   load and execute it.

### Where to Go From Here

Since you've made it this far, you must be interested. You can learn more about
the language from the [Magpie posts](http://journal.stuffwithstuff.com/category/magpie/) on my blog. (Take anything from before 2010 with a grain of salt. It used to be... different.)

To get a sense of the language itself, take a look at some [sample scripts](http://bitbucket.org/munificent/magpie/src/tip/script/), or the [tests](http://bitbucket.org/munificent/magpie/src/tip/test/).

If you have questions or comments, I've created a mailing list
[here](http://groups.google.com/group/magpie-lang), or you can file issues or
send me a message on bitbucket. You can also email me directly using my name
(<tt>Bob</tt>) at my personal domain, <tt>stuffwithstuff.com</tt>.

Cheers, and have fun playing with it!