^title How Do Multimethods Work?

There are a number of tricky challenges to getting multimethods working. They are:

## How Are Return Types Determined?

Different specialized methods should be able to return different types. That way, for example, a `+` method on numbers can return a number, while a `+` specialized to strings can return strings. Dynamically, that's easy. But the static checker needs to be able to determine this too.

This is actually fairly straightforward to solve. During type-checking, we'll statically determine the set of methods that *could* match, based on the known static types of the arguments. The return type of the method from the type-checker's perspective is then the union of those types. For example:

    :::magpie
    def double(n Int -> Int) n * 2
    def double(s String -> String) s ~ s

    var foo = double(123)

Here, the type-checker can determine that `foo` is an Int because the only method whose type is a subtype of the type of `123` is the Int one. A slightly more complex example:

### Omitting Covered Methods

To be as accurate as possible, it would be ideal if it was smart enough to not include the return type of methods that can be statically determined to be completely covered by another one:

    :::magpie
    def add(left Int, right Int -> Int) left + right
    def add(left Object, right Object -> String) left ~ right

    def foo = add(1, 2)

Here, it should know that `foo` is an Int because even though both methods could match, the first one covers the second.

## How Are the Methods Ordered?

Since methods can be defined pretty much anywhere in Magpie it's hard to figure out which order the specialized methods should be tested. CLOS handles this by prefering the most specialized methods first, but "most specialized" probably isn't well-defined in Magpie with interfaces.

One radical option is to just ditch interfaces and go with a more CLOS-like multiple inheritance of classes approach. I'm not crazy about the whole mixins/delegates system anyway, so that might be an improvement.

## How Do We Ensure At Least One Pattern Will Match?

One of the basic and most useful features of the static checker is catching errors like:

    def foo(i Int) ...
    foo("not int")

In the presence of multimethods testing that gets a little funkier. I think this case will actually be easy. All we do is the same logic to statically determine which cases *could* match. If the set is empty, then we report an error. That should cover cases like the above.

## How Do Abstract Methods Work?

One really nice feature of OOP languages is the ability to define abstract methods in base classes. The type-checker can then ensure that all derived classes implement that method.

Magpie accomplishes something similar with interfaces. If you try to pass a concrete type where an interface is expected, it will statically verify that the type satisfies the interface. We definitely do *not* want to lose the ability to say "Any type used in this context requires this capability."

There's two components to this. First, we need to be able to define abstract methods in base classes. Then the type checker must ensure that for all derived classes, there are specialized methods that completely cover that one. Determining total cover in multimethods may be a bit tricky, but I hope it's resolvable. (That feature is also needed to ignore the return type of completely covered methods.)

With that, we can generate static errors when a derived class doesn't implement all of the abstract methods it inherits.

The second piece is ensuring that classes that have abstract methods can't be constructed and passed around. Ensuring that Derived implements Base's abstract methods isn't very helpful if you could actually end up with an instance of just Base that you're trying to dispatch on.

Not sure how to handle that yet. The solution may just be, "don't do that" and generate an error at runtime on a failed match.

## How Do Interfaces Work?

A very common use case in Magpie and other OOP languages is to define a function
that takes an argument of any type that implements a certain interface. In other words, any object that has certain capabilities. Basing this on interfaces instead of base classes dodges the brittle base class problem. Magpie's current interface system makes it even more flexible since existing classes can retroactively implement new interfaces.

How does this translate to multimethods? For example:

    :::magpie
    def ~(left Stringable, right Stringable)
        String concat(left string, right string)
    end

The goal here is that this method can be called on any arguments that have a `string` getter, and that it should be a static error to try calling with an argument that doesn't.

Without interfaces, what is the type signature of that function?

One option would be the C++ solution. An "interface" becomes just a class with no state and some abstract methods. Classes that implement that interface would have to explicitly inherit from it. Then, the existing support for making sure abstract methods are covered would cover this too.

It would look something like:

    :::magpie
    class Stringable
    end

    def abstract string(arg Stringable -> String)

    Int extends(Stringable)

    def string(arg Int -> String) ...

## How Do Constructors Work?

CLOS doesn't really place much emphasis on constructors. When you define a class, you instantiate it by calling:

    :::lisp
    (make-instance 'my-class :slot1 "blah" :slot2 "boof")

So basically you specify the class and all of the slot values, much like `construct()` in Magpie. We can take that and make it nicer by having a class
define a `new` method that specializes the `new` generic function with the class being constructed, and a record of its fields. That would get us to what CLOS supports. If you want to define your own ctor, just create a different `new` method that specializes on the class and a different set of arguments, like so:

    :::magpie
    class Point
        var x Int
        var y Int
    end

    // The above gives us:
    Point new(x: 1, y: 2) // i.e. new(Point, (x: 1, y: 2))

    // If we want a different ctor, just do:
    def class method Point new() new(0, 0)

    Point new() // i.e. new(Point, ())

## How Do Chained Constructors Work?

The above handles classes with no inheritance (or at least no base classes that in turn require construction). What about ones that do?

The CLOS answer is to just union the fields together. If `Base` has field `a` and `Derived` has `b`, to instantiate a derived, you'd do this:

    :::magpie
    Derived new(a: "a", b: "b")

This is because CLOS doesn't emphasize encapsulating state, but I'm really not crazy about that, especially when it comes to private fields. A class should own its internal state and should be able to use its constructor as the interface for that.

One option would be to chain constructors like this:

    :::magpie
    class Base1
        var a
    end

    class Base2
        var d
    end

    class Derived : Base1, Base2
        var f
    end

    // By default, this defines:
    Derived new(Base1: (a: 123), Base2: (d: 234), f: 345 -> Derived)

    // If we want to use different ctors for base:
    def class method Base1 new(b Int, c Int -> Base1) new(b + c)
    def class method Base2 new(e String -> Base2) new(Int parse(e))

    // Now we can do:
    Derived new(Base1: (100, 23), Base2: "234", f: 345)

This is a little fishy though because constructors *create* objects instead of *initializing* them. So when we call the constructors for `Base1` and `Base2`, we'll create two objects and then, I guess, have to copy those fields over to the "real" one. That feels kind of gross. Ignoring that for now, though, the semantics for built-in `new` would be:

    obj = new empty object
    obj.class = the class
    for each property in arg record
        if property name is a base class name
            invoke new with the base class and the property's value
            get the resulting object and copy its fields to obj
        else if property name is a field on the class
            initialize the field with the property value
        end
    end

### Initialize Instead of Construct

Now that I think about it, we may be able to resolve the fishiness by making `new` just initialize `this` instead of returning a new one. In the above examples, all of the `new` methods return their class type, and the ultimate built-in `new` does too. Instead of that, we could replace `new` with `init`. That doesn't return anything. Instead, the expectation is that any `init` method will eventually bottom out on the built-in one.

The built-in `init`, like `new`, takes a record of field values and base class values. Now there is a single built-in `new` method. Its semantics are:

    create an empty object, _newObj
    call init(), passing in the argument passed to new()
    return _newObj

The built-in `init()` does:

    for each property in arg record
        if property name is a base class name
            invoke init() with the base class and the property's value
        else if property name is a field on the class
            initialize the field on _newObj with the property value
        end
    end

Problem solved, I think. This also cleans things up a bit. It always felt tedious to have explicitly declare the return type of `new()` to be the created class.

### What About Factories?

What intended nice feature of the existing `new()` semantics is that a constructor doesn't have to always create and return a new object. It can return a cached one or a subclass or pretty much anything. That's a nice feature to maintain.

I *think* we can support this with the above idea too. All you would need to do is specialize `new()` with a different argument type and you'll hit that method instead. If you want to swap out the "real" `new()` with one that takes the *same* argument type, well... we'll have to see if it's possible to replace a method.


