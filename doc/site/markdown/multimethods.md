^title Multimethods

Methods are the workhorses of Magpie. Most of the code you write will reside inside a method, and most of that code will in turn be calls to other methods. A *method* is an executable chunk of code (an [expression](expressions.html) to be precise) that is bound to a *name* and has a [*pattern*](patterns.html) that describes the *argument* it expects.

## Defining Methods

Methods are defined using the `def` keyword.

    :::magpie
    def greet()
        print("Hi!")
    end

Here we've defined a method named `greet` whose body is a [block](blocks.html) containing a single `print` call. You can call this method like so:

    :::magpie
    greet()

We can define a method that takes an argument by using a pattern in its definition.

    :::magpie
    def greet(who)
        print("Hi, " + who)
    end

    greet("Fred") // Hi, Fred

In this case, the pattern is a simple variable pattern, but more complex patterns can be used:

    :::magpie
    def greet(who is String, whoElse is String)
        print("Hi, " + who + " and " + whoElse)
    end

    greet("Fred", "George")

Here we have a record pattern with two fields that must both be strings. We call it by passing it a record of two strings: `"Fred", "George"`. This may seem a bit strange, but it's important to note that we are *not* passing two arguments. In Magpie, all methods (and [functions](functions.html)) always take a *single* argument. It's just that the argument may be a record which the method destructures.

The end result is that it does what you expect, but there's some conceptual unification going on under the hood. The destructuring initialization that you can do when declaring [variables](variables.html) is the exact same process use when splitting out argument to a method, or selecting a `catch` clause when an [error](error-handling.html) is thrown.

## Left and Right Arguments

Methods are *infix* expressions, which means that an argument may appear to the left of the name, to the right, or both. (More pedantically, *the record that forms the single argument* may have fields which appear to the left and right of the name.)

The `greet` methods we've defined only have a right argument. Methods which only have a *left* argument are informally called *getters*.

    :::magpie
    def (this is String) empty?
        this count == 0
    end

This defines a getter `empty?` (the `?` is just part of the name) whose left argument must be a string. It takes no right argument. It can be called like this:

    :::magpie
    "not empty" empty? // false

And, finally, methods can have arguments on both sides:

    :::magpie
    def (this is String) greet(other is String)
        print("Hi, " + other + ", I'm " + this)
    end

    "Fred" greet("George")

When defining a method, both the left and right argument patterns, if present, must be in parentheses. When *calling* a method, only the right one must.

## Method Scope

In the above examples, it looks like we're adding methods to the `String` class. In other languages, this is called [monkey-patching](http://en.wikipedia.org/wiki/Monkey_patch) and it's frowned upon. If two unrelated parts of the codebase both declare methods on the same class with the same name, they will collide and do something... probably bad.

In Magpie (as in [CLOS](http://en.wikipedia.org/wiki/CLOS)) methods are not *owned* by classes. Instead, methods reside in lexical scope, just like variables. When you call a method, the method is found by looking for it in the scope *where the call appears*, and not on the class of any of the arguments. When a method goes out of scope, it disappears just like a variable.

    :::magpie
    do
        def (this is String) method()
            print(this + " first")
        end

        "a" method() // a first
    end

    a method() // ERROR!

    do
        def (this is String) method()
            print(this + " second")
        end

        "a" method() // a second
    end

It is impossible to have a method collision in Magpie. If you try to define two methods with the same name and pattern in the same scope, it will [throw an error](error-handling.html). This way, you're free to define methods that are called in a way that appears natural without having to worry about shooting yourself in the foot.

<p class="future">Eh, currently it doesn't check for pattern collisions. But it will.</p>

## Multimethods

In Magpie, all methods are *multimethods*. This is one of the places where the language really steps it up compared to other dynamic languages. In the previous section, we noted that it's an error to define two methods with the same name *and pattern* in the same scope. That qualifier is important. It's perfectly fine to define two methods with the same name but *different patterns*.

    :::magpie
    def double(n is Int)
        n * 2
    end

    def double(s is String)
        s + s
    end

Here we've defined two `double` methods, one on strings and one and numbers. Even though they are defined in the same scope, these don't collide with each other. Instead, these are combined to form a single `double` multimethod containing two *specializations*.

When you [call](calls.html) a multimethod, it looks through the methods it contains and their patterns. It then *selects* the most appropriate pattern, and calls the method associated with it.

    :::magpie
    double(3) // 6
    double("ma") // mama

In simple terms, this means Magpie lets you overload methods, which is pretty unusual in dynamic languages. It's also more powerful than most static languages because it's selecting the most appropriate method *at runtime* where overloading in a language like Java is done at compile time.

Since all methods actually take a single argument, we're free to specialize a multimethod on the left argument, right argument, or both. You can also specialize on different record patterns.

    :::magpie
    def (this is String) double
        this + this
    end

    def (this is Int)
        this * 2
    end

    def double(s is String, s is Int)
        s + s, n * 2
    end

    "left" double   // leftleft
    3 double        // 6
    double(3, "do") // 6, dodo

As long as you don't provide two specializations with the exact same pattern, you are free to define as many as you want. If you call a multimethod with an argument that doesn't match *any* of the specializations, it will [throw](error-handling.html) an error.

    :::magpie
    do
        double(true)
    catch is NoMethodError
        print("We didn't specialize double on bools")
    end

We didn't define a `double` method that accepts a boolean, so when we call it, it will throw a `NoMethodError` which gets caught here to print a warning.

## Linearization

The previous section says that the "most appropriate" method is selected based on the argument. In the examples we've seen so far, only one method is a possible match, so most appropriate is pretty easy. If multiple methods match the argument, we need to determine the *best* one. Magpie (and other languages) call this *linearization*.

    :::magpie
    def odd?(0)
        false
    end

    def odd?(n is Int)
        not(odd?(n - 1))
    end

Here we have an `odd?` multimethod with two specializations. If we call it and pass in `0`, then both specializations match. Which is best? To answer this, Magpie has a few relatively simple rules that it uses to order the patterns.

Before we get to those rules, it's important to understand one thing that does *not* affect ordering: *the order that methods are defined in a program has no affect on linearization.*

### Pattern Kind

First, different kinds of patterns are ordered. From best to worst:

1.  Value patterns
2.  Record patterns
3.  Type patterns
4.  Wildcard patterns

For variable patterns, we look at its inner pattern. (If it doesn't have one, it implicitly has a wildcard one.) The above list resolves our `odd?` example: the first method will win since a value pattern (`0`) takes precedence over a type pattern (`is Int`).

To linearize two patterns of the same kind, we need more precise rules.

### Class Ordering

To order two type patterns, we look at the [classes](classes.html) being compared and see how they are related to each other. Subclasses take precedence over superclasses.

    :::magpie
    defclass Parent
    end

    defclass Child is Parent
    end

    def sayClass(is Parent)
        print("Parent")
    end

    def sayClass(is Child)
        print("Child")
    end

    sayClass(Child new())

Here, both methods match because an instance of `Child` is also an instance of `Parent`. In this case, the second method specialized to `is Child` wins because `Child` is a subclass of `Parent`.

There is a similar rule for sibling classes.

    :::magpie
    defclass ParentA
    end

    defclass ParentB
    end

    defclass Child is ParentA, ParentB
    end

    def sayParent(is ParentA)
        print("ParentA")
    end

    def sayParent(is ParentA)
        print("ParentA")
    end

    sayParent(Child new())

Here, `ParentB` will win because comes *after* `ParentA` in `Child`'s list of parents. Because Magpie only allows a single inheritance path to any class, these two rules are enough to order any pair of classes that are related to each other.

### Record Ordering

It's possible for two record patterns to match the same argument.

    :::magpie
    def printPoint(x: x, y: y)
        print(x + ", " + y)
    end

    def printPoint(x: x, y: y, z: z)
        print(x + ", " + y + ", " + z)
    end

    printPoint(x: 1, y: 2, z: 3)

A record pattern matches as long as the argument has the fields the record requires. *Extra* fields are allowed and ignored, so here both methods match.

The rules for ordering records are a bit subtle. The first requirement is that the records must specify the same fields, or one must be a subset of the other. If not, they cannot be ordered and an `AmbiguousMethodError` is [thrown](error-handling.html).

    :::magpie
    def say(x: x)
        print("x " + x)
    end

    def say(y: y)
        print("y " + y)
    end

    say(x: 1, y: 2)

It's unclear what the programmer was even trying to accomplish here, and Magpie can't read your mind. So in cases like this, it just raises an error to signal its confusion.

Our first example doesn't have this problem, though. The first definition of `printPoint` is a subset of the former, so there's no ambiguity. In that case, it proceeds to the next step. We go through the fields that the two records have in common and linearize their patterns. If all of those fields order the same way, then the record whose fields wins is the winner. If they disagree, it's ambiguous.

**TODO still ambiguous as to which printPoint wins here.**

<p class="future">There are still some details and corner cases that I'm working on here, so this may change a bit over time though the basics seem pretty solid.</p>


**TODO**

## Indexers

**TODO**

**TODO: Also need to document how they interact with modules.**
