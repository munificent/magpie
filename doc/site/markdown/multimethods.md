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

Methods are *infix* expressions, which means that an argument may appear to the left of the name, to the right, or both. (More pedantically, the *record that forms the single argument* may have fields which appear to the left and right of the name.)

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

**TODO**
## Linearization

**TODO**

## Indexers

**TODO**

**TODO: Also need to document how they interact with modules.**
