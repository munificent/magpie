^title What is a Multimethod's Identity?

The key question with multimethods is one of identity. A multimethod is a collection of methods, but when should two piles of identically-named methods be a single method and when should they be separate?

Other ways to phrase the question:

1. When do two `def`s with the same name collide and when do they merge?
2. When do two `import`s of methods with the same name collide, and when do they merge?
3. When a method is defined in one module, which other modules see that change?

## Current Implementation

The current implementation works like this:

Imagine the `import` graph of a set of modules. If module `a` imports `b` then there's a *directed* edge from `a` to `b`. Two methods are part of the same multimethod if there is a path from one to the other, or if they each have a path to a third shared method.

For example:

           .----------.
           | // a.mag |
           | def m1() |
           '----------'
            /        \
    .----------. .----------.
    | // b.mag | | // c.mag |
    | import a | | import a |
    | def m1() | | def m1() |
    | def m2() | | def m2() |
    '----------' '----------'

Here, there is only a single `m1` method across all three modules, but `b` and `c` each have their own `m2`. Note that if an `m2` were later added to `a`, then all three would collapse into a single multimethod.

## Use Cases and Problems

There are a number of relevant use cases:

### Overloading

This should not be an error:

    :::magpie
    def method(n Int) ...
    def method(s String) ...

But this should:

    :::magpie
    def method(n Int) ...
    def method(s Int) ...

### Unrelated Methods

Given this:

    :::magpie
    // a.mag
    def method(n Int) ...

    // b.mag
    def method(n Int) ...

As long as those two modules are never both imported unqualified by another module, the above should be fine and produce no error. Those methods should be oblivious to each other.

### Overriding

    :::magpie
    // a.mag
    def method(any) ...

    def callIt(arg) method(any)

    // b.mag
    import a

    def method(n Int) "int"
    callIt(123) // should return "int"

Here module `a` defines a multimethod `method`. Module `b` refines it. The important part is `callIt()`. It exists only in module `a` and isn't aware of module `b` at all. But when it's called, it *should* still successfully find the more specific method defined in `b`.

The specific case where this arose is:

    :::magpie
    // core.mag
    def (left Comparable) < (right Comparable)
        left compareTo(right) == -1
    end

    // spec.mag
    defclass TestComparable
        // ...
    end

    def (left TestComparable) compareTo(right TestComparable) ...

    var test = TestComparable new()
    test < test

The last line calls `<` which in turn calls `compareTo` from `core.mag`. But since `core.mag` didn't know about `TestComparable` at all, it never saw that specialization in `spec.mag`.

The fix was to import entire multimethods on `import`. So when `spec.mag` imported `core.mag` it got a reference to the `compareTo` multimethod&mdash; the actual same object that the `core.mag` module was referencing. When we defined `compareTo` on `TestComparable`, that method went into that same multimethod object, so `core.mag` was later able to see it.

### Colliding Getters

Every field on a class has a corresponding getter, which is just a multimethod. Given that, consider:

    :::magpie
    defclass Person
        var name String
    end

    defclass Pet
        var name String
    end

In the current implementation, this code in a single module will implicitly create a single `name` multimethod with specializations for `Person` and `Pet`, so it works as expected. Now consider:

    :::magpie
    // a.mag
    defclass Person
        var name String
    end

    // b.mag
    defclass Pet
        var name String
    end

    // c.mag
    import a
    import b

Those imports will collide when they both try to import distinct and unrelated `name` multimethods. One possible solution is to have those imports merge and create a single `name` multimethod. As long as none of the specializations collide (which they don't here), that would be fine.

But now consider the previous overriding use case. Consider:

    :::magpie
    // a.mag
    defclass Person
        var name String
    end

    // b.mag
    defclass Pet
        var name String
    end

    // c.mag
    import a
    import b

    def (_ Int) name ...

Which multimethod do we define that last `name` in? The one from `a` or `b`, or both?

Another example of the problem:

    :::magpie
    // a.mag
    def method(s String) "string"
    def callFromA(arg) method(arg)

    // b.mag
    def method(b Bool) "bool"
    def callFromB(arg) method(arg)

    // c.mag
    import a
    import b

    def method(n Int) "int"

    method(true) // should be "bool"
    callFromA(123) // "int"?
    callFromB("str") // "string"?

Maybe the way to phrase the question is: are methods lexically scoped or dynamically scoped? This last example implies a certain amount of dynamic scoping: `callFromA()` should have access to the `method` methods defined where `callFromA()` is being *called*. But that kind of seems like crazy talk.

### Chained Imports

Imports are not and should not be transitive. If I import `a` which imports `b`, I don't get everything in `b` imported into my module, just the stuff from `a`. If we were to try to dynamically scope methods, though, that would break it. Consider:

    :::magpie
    // a.mag
    def aMethod() "a"

    // b.mag
    import a

    def bMethod() aMethod()

    // c.mag
    import b

    bMethod() // should return "a"

When we call `bMethod()`, we can look it up in module `c` because it's been imported. But when that in turn looks up `aMethod()`, we can't look that up in `c`, because `aMethod()` hasn't been imported into it.

## Solutions

### No Spanning Across Modules

The simplest solution is that multimethods are never shared across modules. Instead, each module has its own multimethod for a given name. When you import, the methods are imported individually and piled into that collection. That addresses overloading, colliding getters, and unrelated methods. It's also concurrency friendly (since defining a method in one module doesn't affect others.

It breaks overriding. As far as I can tell, that's the only real problem with this, though that's certainly a valid one.

    :::magpie
    // core.mag
    def (left Comparable) < (right Comparable)
        left compareTo(right) == -1
    end

    // spec.mag
    defclass TestComparable
        // ...
    end

    def (left TestComparable) compareTo(right TestComparable) ...

    var test = TestComparable new()
    test < test


### Global Multimethods

The interpreter keeps a global pool of multimethods. Any method defined with a given name in any package becomes part of the same multimethod.

If you haven't defined a method with a given name, or imported it, you won't see that name at all, but if you have, you see the same one as every other module.

This solution is pretty simple, and addresses every use case lists above except for unrelated methods. It also doesn't allow lexically-scoped multimethods, but it could be that this "global pool" rule only applies to top-level multimethods or something.

But, of course, unrelated methods were one of the main motivations for multimethods in the first place.

### Current Solution

The current solution works pretty well. When you import a multimethod, you
import the exact same object, so when you add new methods, the original module can see them too. That addresses overriding while still allowing unrelated methods.

The only real problem with it is colliding getters. The CLOS solution is to just rename:

    :::magpie
    // a.mag
    defclass Person
        var name String
    end

    // b.mag
    defclass Pet
        var name String
    end

    // c.mag
    import a = a
    import b = b

    Person new("Bob") a.name
    Pet new("Ginny") b.name

That's perfectly valid for most methods. It just feels a bit weird to have to do it for getters. One angle to look at it is, "if two classes have the same field, should you be able to treat them generically?" Consider:

    :::magpie
    defclass Person
        var name String
    end

    defclass Pet
        var name String
    end

    def sayName(who) print(who name)

Should we expect `sayName()` to work with both people and pets? If the answer is yes, then renaming is wrong. If it's no, then it's reasonable. If you *should* be able to act on those classes generically, then one solution is:

    :::magpie
    defclass Named
        var name String
    end

    defclass Person : Named
    end

    defclass Pet : Named
    end

    def sayName(who Named) print(who name)

That's probably the Right Thing, and not that this also fixes the colliding getter problem. Even if `Person` and `Pet` are defined in different modules, they will both be importing the one that defines `Named` so they'll use the same multimethod for `name`.

So maybe the current system is the best we can do.