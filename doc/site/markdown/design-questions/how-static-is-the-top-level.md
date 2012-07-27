^title How Static is the Top Level of a Module?

Languages basically fall into one of two buckets:

1.  Languages where the top level of a file is pure definitions: types and
    functions. This includes, C, C++, Java, C#, and Dart. With these, the
    program's definitions are "timeless": the order that they appear in the
    text doesn't matter or affect anything. This makes cyclic dependencies and
    mututal recursion simple because you can handle all definitions "all at
    once".

2.  Languages where the top level of a file is a series of imperative
    statements or expressions. This includes most scripting languages like
    Scheme, Ruby, Python, JavaScript, etc. With these, "definitions" are just
    regular imperative code that is executed top-to-bottom like other stuff.

    These languages have to do extra work to enable mutual recursion: Scheme
    treats top-level definitions like a letrec, JavaScript has hoisting, and I
    think Ruby and Python just late bind everything at the top level.

I'll call these buckets "static" and "dynamic" because in the first bucket, you
can figure out everything a program defines statically without executing any
code. I need to decide which bucket Magpie is in. The interpreter is currently
in the dynamic bucket, but I don't know if that's ideal.

## Static pros:

*  It makes static whole program compilation much simpler: the compiler doesn't
   need to be able to evaluate code.

*  Mutual recursion works "naturally".

*  Cyclic dependencies between modules are simpler to handle.

*  It makes it easier to compile a multimethod (and all of its methods) to
   bytecode all at once.

*  It's more tool-friendly: it's simpler to build an IDE that statically
   understands a program's types.

*  It isn't overly flexible. It usually only allows a subset of what a dynamic
   language would allow. This means you aren't painting yourself into a corner:
   if you start with a subset, you can always expand that later, but you can't
   take stuff away.

## Dynamic pros:

*  It lets you have imperative code at the top level, which is nice for simple
   scripts: no special `main()` entrypoint.

*  It lets patterns contain expressions without much trouble. Expression-like
   things appear in a bunch of places in definitions: superclasses, method
   patterns, and field patterns.

*  It's simpler to implement (I think). Since expression-like things often
   appear in definitions, most static languages end up with a sublanguage that
   can be executed at "compile time": const expressions in Dart, etc. Having to
   define and implement that sublanguage is more work. If you just do
   everything dynamically, you only do one language.

*  It's more flexible. You can do things like have arbitrary expressions in
   method patterns without too much trouble.

## What should Magpie do?

I'm leaning towards the dynamic side, for a bunch of reasons:

*   Magpie is dynamically typed, so it feels like a more natural fit. Magpie is
    spiritually closer to Ruby, Io, and Scheme than Java or C#.

*   Top-level variables look like local variables, so I think users will expect
    them to evaluated top-down in a program.

*   We want to support imperative code at the top-level of a program. It feels
    a bit strange to have that but have other definitions not be imperative.

*   At some point, we may want to support nested class and methods inside local
    scopes (much like how Scheme allows `define` inside a `begin` block). Since
    that will be in the middle of imperative code, it seems more consistent for
    the top level to feel dynamic too.

*   The tooling pros for something more static are likely moot for Magpie since
    there is no Magpie IDE. I think it's easier to get people excited about a
    hobby language that can do fun dynamic stuff now, than one that may be able
    to be tooled better later.

## Given that, what are the semantics of top-level code?

Here's some constraints:

1.  Definitions should be executed in the order in which they appear in a
    module. Things like `var` initializers can have visible side-effects, so
    the order matters.

2.  Mutual recursion should work. Of course.

3.  Cyclic module dependencies should be allowed, at least in some cases. Given
    type patterns, it's entirely likely for two modules to refer to each other.

4.  Method pattern collisions should be detected as early as possible. We can
    relax this, but the earlier the better here.

Some motivating examples:

### The order of `var` expressions matters:

    var a = 0
    var b = a = a + 1
    var c = a = a + 1

### The order of `var` expressions matters, not because of assignment:

    var a = writeSomeFile()
    var b = readThatFile()

### The order between `defclass` and `var` matters:

    var foo = "not base"
    defclass Base
    end
    var bar = foo = Base
    defclass Derived is foo
    end

### The order between `def` and `var` matters:

    var foo = "one"
    def bar(== foo) "1"
    var bar = foo = "two"
    def bar(== foo) "2"

### The order between `def` and `var` matters (2):

    def bar(any) "any"
    var foo = bar(1) // "any"
    def bar(1) "one"
    var baz = bar(1) // "one"

### A safe cyclic dependency:

    // a.mag
    import b

    defclass A
    end

    def (a is A) collideWith(b is B) ...

    // b.mag
    import a

    defclass B
    end

    def (b is B) collideWith(a is A) ...

### An unsafe cyclic dependency:

    // a.mag
    import b
    var a = b

    // b.mag
    import a
    var b = a

### Something nasty

    // a.mag
    import b

    defclass A
    end

    def foo(== B) A
    def (a == foo(B)) collideWith(b is B) ...

    // b.mag
    import a

    defclass B
    end

    def (b is B) collideWith(a is A) ...

## Temporal dependencies

The key idea is separating out regular dependencies and *temporal ones*. A regular dependency is just one module importing another. It means that, by the time a method in that module is called, the other one needs to have its top-level definitions available.

A temporal dependency is a stricter requirement: if module A has a temporal dependency on B then module B's top level code must be executed before A's.

You can have circular dependencies, but you cannot have circular *temporal* dependencies.

If we can make enough things *not* induce a temporal dependency, then cyclic imports will work fine. These should induce a temporal dependency from A to B:

*   If a `var` initializer in A refers to a variable or class in B.
*   If a superclass expression in A refers to a variable or class in B.

These should *not*:

*   If a method pattern in A refers to a variable or class in B.
*   If a field pattern in A refers to a variable or class in B.

The trick is squaring the last two with the fact that method patterns may contain expressions with visible side effects.

**TODO: Figure out how we can make this work.**

Ideas:

* Does only allowing name expressions whose variables are single-assignment help?

## A strawman for how the top level code is compiled

Traverse all imports to build up the full set of needed modules. Cyclic dependencies are allowed: this is just walking the graph.

For each module, forward declare all of its methods. In other words, add a multimethod to the global pool with the method's name, but don't actually add any concrete methods. This is just to detect calls to completely unknown methods at compile time.

For each module, get all of the variables it defines: all of the `var` and `defclass` top-level expressions. With this, for any given name in a given module, we can tell where it came from: either in this module or in one of its imports.

This should give us enough information to determine temporal dependencies between modules. Do that and then topologically sort that so that a module is always sorted after its temporal dependencies. If there is a cycle, report an error and stop.

Otherwise, execute the modules in the resulting order.
