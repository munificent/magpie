^title Hacking On Magpie
^index 4

If you'd like to contribute to Magpie, or just poke around at its internals, this guide is for you. A word of warning before we start: Magpie is under heavy development. As soon as I save this doc, I'll probably edit some code and invalidate something here. I'll try to keep this up-to-date, but understand that there will likely be some lag.

### The Code

The Magpie interpreter is written in straight Java. It has no external dependencies and doesn't really do any magical stuff in the language. Since it changes so frequently, the code isn't as well-documented as I'd like, but it's getting there. You should be able to figure out what's going on without too much trouble.

#### Cloning the Repo

Magpie lives on [bitbucket](http://bitbucket.org). To work on the source, just clone the repository from: [http://bitbucket.org/munificent/magpie](http://bitbucket.org/munificent/magpie). That will give you a source tree with:

* `/base`: Contains the base library: the Magpie code automatically run at startup.
* `/doc`: Documentation, of course. Lots of notes to myself and stuff that may be outdated, but may be useful.
* `/doc/site`: The markdown text and python script used to build this site.
* `/old`: The old pre-Java, C# statically-typed version of Magpie.
* `/script`: Example Magpie scripts.
* `/src/com/stuffwithstuff/magpie`: The Java source code of the interpreter.
* `/test`: The test suite, the set of Magpie scripts used to verify the interpreter works correctly.

### The Namespaces

The code for the interpreter is split out into six namespaces:

* `com.stuffwithstuff.magpie`: Top-level stuff for the application: `main()`, the REPL, file-loading, etc.
* `com.stuffwithstuff.magpie.ast`: Classes for the parsed syntax tree. The code here defines the data structures that hold Magpie code.
* `com.stuffwithstuff.magpie.interpreter`: The interpreter. The heart of Magpie is here.
* `com.stuffwithstuff.magpie.interpreter.builtins`: Built-ins. Methods in Magpie that are implemented in Java live here.
* `com.stuffwithstuff.parser`: The lexer and parser. This takes in a string of code and spits out AST.
* `com.stuffwithstuff.util`: Other random utility stuff.

### A Tour Through the Code

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
    The source code is now in a form that Magpie can understand. Each expression
    will be a subclass of the base `Expr` class. There are subclasses for all
    of the core expression types (i.e. the things that don't get desugared 
    away): literals (`IntExpr`, `StringExpr`, etc.), messages (`MessageExpr`),
    flow control (`IfExpr`, `ReturnExpr`, etc.), etc.

    The actual set of `Expr` subclasses is in flux. It isn't well-defined what
    becomes a real AST node, and what gets desugared by the parser into 
    something simpler. Having fewer AST classes makes the interpreter simpler to
    implement. Having more makes it easier to create good error messages. What
    will likely happen over time is that this will be split into two levels: a
    rich AST set that is very close to the text syntax. That will get translated
    to a much simpler core syntax (basically just messages and literals) which
    will be what the interpreter or compiler sees.

4.  Next, we create an `Interpreter` to actually interpret the `Expr`s. It
    creates a global `Scope`, and defines the built-in types in it: `Int`,
    `Nothing`, etc.

5.  The `Interpreter` then registers the built-in methods on those types. For
    each built-in Magpie class (`Int`), there is a corresponding static Java
    class that has the built-in methods for it (`IntBuiltIns`). Each method in
    that class has a `Signature` annotation that describes how the method looks
    to Magpie. `BuiltIn` uses reflection to find all of those methods and make
    them available to be called from Magpie.
    
6. Now we've got a live Magpie environment we can start running code in, but 
   it's still pretty empty. Things like interfaces aren't defined in Java,
   they're in the base library. So before we can do useful stuff, we need to
   load the base lib. This is done automatically by `Script` before it runs the
   user-provided code.

7. Finally we can throw our code at the interpreter. We pass it our parsed
   expressions. It creates an `EvalContext` which defines the context in which
   code is executed: the local variable scope (and its parent scopes), as well 
   as what `this` refers to. Code is always evaluated with an `EvalContext`.

8.  It then creates an `ExprEvaluator`. `Expr` uses the Visitor Pattern to allow
    operations on the different expression types without having to put that code
    directly in the `Expr`-derived classes. `ExprEvaluator` is one of the two
    visitors in Magpie. It, as you'd expect, evaluates `Expr`s. This is where
    the actual Magpie code gets interpreted.
   
    Right now, Magpie is a tree-walk interpreter: it interprets code by
    recursively traversing an expression tree and evaluating the nodes. At some
    point, it will likely compile to bytecode instead. When that happens,
    `ExprEvaluator` will become `ExprCompiler`, which will walk the AST and
    generate bytecode.

9.  We pass our expressions to evaluate to the evaluator which evaluates them.
    Ta-da! We've run Magpie code. Now this is where things get interesting. At
    this point, we've evaluated the script given to us. When that's done, it's
    possible that that script defined a function and assigned it to the global
    variable `main`. If that's happened, it's time to type-check...
    
10. `Script` now creates a `Checker`. This class is the static sister to 
    `Interpreter`. Where the interpreter evaluates code dynamically, the checker
    examines its statically. We pass the interpreter to checker (because it has
    the global scope where everything we're checking is defined) and tell it to
    check.

11. `Checker` creates a new top-level `EvalContext`. `EvalContext`s in the
    interpreter bind variable *values* to names. In the `Checker`, we'll use the
    same `EvalContext` class, but we'll be binding *types* to names. (We can
    re-use `EvalContext` here because all types are first-class in Magpie: a 
    type is just another Magpie object, so it *is* a value.
    
    Then it walks through all of the classes and functions remaining in global
    scope and checks them.

12. To check a function, we create an `ExprChecker`. This is the other `Expr`
    visitor class. It walks a tree of `Expr`s and type-checks them. The most
    important part of type-checking is that when it encounters a `MessageExpr`,
    it looks at the type of the receiver to see if it's a valid message for that
    type. Then it looks at the arguments being passed to make sure they match
    the type annotations provided for the parameters.
    
13. Right here is where Magpie is very different from most languages. The core
    of type-checking is looking at type compatibility: you have a method that's
    declared to take a `Foo` and you're passing it a `Bar` is that OK?
    
    In most languages, the rules for answer that question (the subtype relation)
    are hard-coded in the interpreter or compiler. In Magpie, they're actually
    implemented in Magpie (in the base lib). This means that in the middle of
    *statically* checking Magpie code, we need to periodically switch to 
    *dynamically* evaluating some Magpie code.
    
    When we check a function, it will periodically call over to the interpreter
    to tell it to *evaluate* some expression: a type annotation or a
    `canAssignFrom` message which is what Magpie calls the subtype test. This is
    the most magical part of Magpie's code.

14. If any of these type-checks fail, we generate a runtime error. Once 
    everything is checked, those errors are returned to `Script`. If there are
    any, it prints them and stops. (Yay! Static type-safety in a dynamic 
    language!) Otherwise, it then invokes `main()`.

15. We're done!