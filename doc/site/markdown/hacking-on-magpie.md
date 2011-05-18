^title Hacking On Magpie

If you'd like to contribute to Magpie, or just poke around at its internals, this guide is for you. A word of warning before we start: Magpie is under heavy development. As soon as I save this doc, I'll probably edit some code and invalidate something here. I'll try to keep this up-to-date, but understand that there will likely be some lag.

## The Code

The Magpie interpreter is written in straight Java. It has no external dependencies and doesn't really do any magical stuff in the language. Since it changes so frequently, the code isn't as well-documented as I'd like, but it's getting there. You should be able to figure out what's going on without too much trouble.

### Cloning the Repo

Magpie lives on [github](http://github.com). To work on the source, just clone [the repository](http://github.com/munificent/magpie). That will give you a source tree with:

* `/base`: Contains the base library: the Magpie code automatically run at startup.
* `/doc`: Documentation, of course. Lots of notes to myself and stuff that may be outdated, but may be useful.
* `/doc/site`: The markdown text and python script used to build this site.
* `/old`: The old pre-Java, C# statically-typed version of Magpie.
* `/script`: Example Magpie scripts.
* `/spec`: The Magpie spec, the executable specification that defines and verifies the language.
* `/src/com/stuffwithstuff/magpie`: The Java source code of the interpreter.

## The Namespaces

The code for the interpreter is split out into six namespaces:

* `com.stuffwithstuff.magpie`: Classes that define the visible programmatic interface to the language. If you host Magpie within your own app, you'll talk to Magpie through here.
* `com.stuffwithstuff.magpie.app`: Stuff for the standalone application: `main()`, the REPL, file-loading, etc.
* `com.stuffwithstuff.magpie.ast`: Classes for the parsed syntax tree. The code here defines the data structures that hold Magpie code.
* `com.stuffwithstuff.magpie.interpreter`: The interpreter. The heart of Magpie is here.
* `com.stuffwithstuff.magpie.intrinsics`: Built-in methods in Magpie that are implemented in Java live here.
* `com.stuffwithstuff.parser`: The lexer and parser. This takes in a string of code and spits out AST.
* `com.stuffwithstuff.util`: Other random utility stuff.

## A Tour Through the Code

The simplest way to understand Magpie's code is to walk through it in the order that it gets evaluated.

1.  We start in `Magpie:main()`, of course. All it does is parse the
    command-line args and then either start a REPL, run the test suite, or
    execute a script. Let's assume we're running a script for now.

2.  Once we've loaded a .mag file into a `String`, it gets passed to the lexer:
    `Lexer`. That class takes in a `String` and chunks it into a series of
    `Token`s. A Token is the smallest meaningful chunk of code: a complete
    number, keyword, name, operator, etc.

3.  The `Token` stream is fed into the parser: `MagpieParser`. This is a simple
    recursive-descent parser with a fixed amount of lookahead. This class
    contains the core Magpie grammar. Generic parsing functionality is in its
    base class: `Parser`. Most of Magpie's high-level grammar is split out by
    keyword. When `MagpieParser` encounters a keyword like `var`, `def`, or
    `class`, it passes off functionality to an `ExprParser` that is registered
    to that keyword. Eventually, these will likely be implemented in Magpie so
    that its syntax can be extended by users.

    The end result of this is single expression or list of expressions.
    The source code is now in a form that Magpie can understand. Each expression will be a subclass of the base `Expr` class. There are subclasses for all of the core expression types (i.e. the things that don't get desugared away): literals (`IntExpr`, `StringExpr`, etc.), calls (`CallExpr`), flow control (`MatchExpr`, `ReturnExpr`, etc.), etc.

    The actual set of `Expr` subclasses is in flux. It isn't well-defined what
    becomes a real AST node, and what gets desugared by the parser into
    something simpler. Having fewer AST classes makes the interpreter simpler to implement. Having more makes it easier to create good error messages. What will likely happen over time is that this will be split into two levels: a rich AST set that is very close to the text syntax. That will get translated to a much simpler core syntax (basically just messages and literals) which will be what the interpreter or compiler sees.

4.  Next, we create an `Interpreter` to actually interpret the `Expr`s. It
    creates a global `Scope`, and defines the built-in types in it: `Int`,
    `Nothing`, etc.

5.  The `Interpreter` then registers the built-in methods on those types. For
    each built-in Magpie class (`Int`), there is a corresponding static Java
    class that has the built-in methods for it (`IntBuiltIns`). Each method in
    that class has a `Signature` annotation that describes how the method looks
    to Magpie. `BuiltIn` uses reflection to find all of those methods and make
    them available to be called from Magpie.

6.  Now we've got a live Magpie environment we can start running code in, but
    it's still pretty empty. Things like interfaces aren't defined in Java, they're in the base library. So before we can do useful stuff, we need to load the base lib. This is done automatically by `Script` before it runs the user-provided code.

7.  Finally we can throw our code at the interpreter. We pass it our parsed
    expressions. It creates an `EvalContext` which defines the context in which code is executed: the local variable scope (and its parent scopes), as well as what `this` refers to. Code is always evaluated with an `EvalContext`.

8.  It then creates an `ExprEvaluator`. `Expr` uses the Visitor Pattern to
    allow operations on the different expression types without having to put that code directly in the `Expr`-derived classes. `ExprEvaluator` is one of the two visitors in Magpie. It, as you'd expect, evaluates `Expr`s. This is where the actual Magpie code gets interpreted.

    Right now, Magpie is a tree-walk interpreter: it interprets code by
    recursively traversing an expression tree and evaluating the nodes. At some
    point, it will likely compile to bytecode instead. When that happens,
    `ExprEvaluator` will become `ExprCompiler`, which will walk the AST and
    generate bytecode.

9.  We pass our expressions to evaluate to the evaluator which evaluates them.
    Ta-da! We've run Magpie code.
