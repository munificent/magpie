^title Welcome

Thanks for coming!

Magpie is a small dynamically-typed programming language built around [*patterns*](patterns.html), [*classes*](classes.html), and [*multimethods*](multimethods.html). It tries to blend the syntactic charm of Ruby, the open-ended extensibility of CLOS, and the lightweight concurrency of Go.

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

Magpie has two implementations right now. There is a prototype interpreter
written in Java. This supports more of the language, but is (of course) tied to
the JVM and is *much* slower. It's main job was to let me iterate on the
language semantics quickly.

Now that the language has (mostly) settled down, I've started writing a
bytecode VM in C++. This is the "real" Magpie implementation, but it's still a
work in progress. All current development is going on here. The Java interpreter
is mainly a reference.

### Building the Java Interpreter

1.  **Pull down the code.** It lives here: https://github.com/munificent/magpie

2.  **Build it.** The repo includes an Eclipse project if that's your thing. If
    you rock the command-line, you can just do:

        $ cd magpie
        $ ant jar

### Building the Bytecode VM

1.  **Pull down the code.** It lives here: https://github.com/munificent/magpie

2.  **Install [gyp][].** This is used to generate a makefile, Visual Studio
    solution or XCode project as appropriate for your OS. You can pull it down
    from the repo using:

        $ git clone http://git.chromium.org/external/gyp.git

3.  **Generate a project.** Run gyp from the root directory of the magpie repo:

        $ cd <path to magpie repo>
        $ <path to gyp repo>/gyp --depth=1

4.  **Set the output directory (XCode 4 only).** Recent versions of XCode build
    into some shared directory not related to where the project is. This borks
    Magpie since it's a command-line executable that loads the core library
    from a path relative to that executable.

    Unfortunately, this setting isn't in the project itself, so gyp can't help.
    After you generate the project, open it in XCode, then:

    1. Choose "File > Project Settings...".
    2. On the "Build" tab, click "Advanced...".
    3. Set "Build Location" to "Custom > Relative to Workspace".
    4. Set "Products" to `build`.
    5. Set "Intermediates" to `build/Intermediates`.
    6. Click "Done".

    This should ensure that Magpie gets built into `build/<config>/magpie`.

5.  **Build the project.** Do what you usually do on your OS to build the thing.
    On Mac, that means open the XCode project and build from there. In Windows,
    there is a Visual Studio solution you can build. On Linux, you can just run
    `make`.

[gyp]: http://code.google.com/p/gyp/

### Running Magpie

Magpie is a command line app. After building it, you can run it by doing:

        $ ./magpie

This will run the Java interpreter or the bytecode VM, whichever is more recent.

If you run it with no arguments, it drops you into a simple REPL. Enter a
Magpie expression and it will immediately evaluate it. Since everything is an
expression, even things like class definitions, you can build entire programs
incrementally this way. Here's one to get you started:

    for i in 1..20 do print("<your name> is awesome!")

If you pass an argument to the app, it will assume it's a path to a script
file and it will load and execute it:

    $ ./magpie example/hello.mag

## Where to Go From Here

Since you've made it this far, you must be interested. You can learn more about
the language from the [Magpie posts](http://journal.stuffwithstuff.com/category/magpie/) on my blog. (Note that the language has changed dramatically over time, so the older a post is, the less likely it is to still be relevant.)

If you have questions or comments, the mailing list
[here](http://groups.google.com/group/magpie-lang) is a good place to start. You can [file bugs or issues on github](https://github.com/munificent/magpie/issues). If you want some live interaction, there's an IRC channel on freenode: [#magpie-lang](irc://irc.freenode.net/magpie-lang).

Cheers, and have fun playing with it!
