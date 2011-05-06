^title Classes

Magpie is a class-based language. That means everything you can stick in a
variable will be an *object* and every object is an instance of some *class*.
Even [primitive types](primitives.html) like numbers and booleans are full-featured objects, as are [functions](functions.html).

Objects exist to *store data*, package it together, and let you pass it around. Objects also know their class, which can be used to select one method in a [multimethod](multimethods.html).

Unlike most object-oriented languages, classes in Magpie do not own methods. State and behavior are not encapsulated together. Instead, classes own state, and multimethods own behavior.

## Defining Classes

A class has a *name* and a set of *fields*, which describe the state the class holds. Unlike other dynamic languages, Magpie requires all of a class's fields to be explicitly declared. They aren't just loose bags of data. You can define a new class using the `defclass` keyword.

    :::magpie
    defclass Point
        var x
        var y
    end

This declares a simple class `Point`, with two fields `x` and `y`.

## Constructing Instances

Once you've defined a class, you can instantiate new instances of it by calling the constructor method `new`. The left-hand argument to `new` is the class being instantiated, and the right-hand argument is a [record](records.html). The record has a named field for each field that the class defines. The types of those fields must match the pattern defined for the field if any. Given our above `Point` class, we can create a new instance like this:

    :::magpie
    var point = Point new(x: 2, y: 3)

### Overloading Initialization

Construction actually proceeds in two stages. When you invoke `new()`, it creates a fresh object of your class in some hidden location. Then it invokes the `init()` multimethod, passing in the same arguments. `init()` is responsible for initializing the fields of this hidden instance, but it doesn't return anything. After `init()` is done, `new()` returns the freshly-created and now fully-initialized object.

When you define a class, Magpie automatically creates a new specialization for `init()` that takes your class on the left, and a record of all of its fields on the right. This is called the *canonical initializer*. You can also provide your own specializations of `init()`. Doing so lets you overload constructors.

    :::magpie
    def (this == Point) init(x is Int, y is Int)
        this init(x: x, y: y)
    end

Here we've defined a new `init()` method that takes a `x` and `y` coordinates using a simple unnamed record. We can call it like this:

    :::magpie
    var point = Point new(2, 3)

When you call `new()` it looks for an `init()` method that matches *whatever* you pass to it. In this case, `2, 3` matches our overloaded `init()` method. That method in turn calls the canonical or "real" initializer to ensure that all of the class's fields are initialized.

This way, you are free to overload `init()` to make it easy to create instances of your classes. The only key requirement is that an `init()` method needs to eventually "bottom out" and call the canonical initializer before it returns. The canonical `init()` does the magic of actually initializing all of the fields. 

### Overloading Construction

In the above example, we provide an alternate path to creating a new object, but we're still creating a new object of the given class. Sometimes you may need to be more flexible than that. Perhaps you want to cache objects.

    :::magpie
    def (this == Point) new(x is Int, y is Int)
        match x, y
            case 0, 0 then zeroPoint // Use cached one.
            else this new(x: x, y: y)
        end
    end

As you can see, you can also overload `new()` itself. If you do that, you can sidestep the process of creating a fresh instance entirely and return another existing object.

This also gives you the flexibility of creating an instance of a different class than what was passed in. You may want to hide the concrete class behind an abstract superclass, or switch out the concrete class based on some specific data passed in. Overloading `new()` gives you this flexibility without having to go through the trouble of implementing your own [factory](http://en.wikipedia.org/wiki/Factory_pattern).

## Fields

Once you have an instance of a class, you can access a field by invoking a [getter](calls.html) on the object whose name is the name of the field.

    :::magpie
    var point = Point new(x: 2, y: 3)
    print(point x) // 2

Here, `point x` is a call to a method `x` with argument `point`. As you would expect, it returns the field's value.

### Assigning to Fields

Setting a field on an existing object looks like you'd expect:

    :::magpie
    var point = Point new(x: 2, y: 3)
    point x = 4
    print(point x)

**TODO: Explain that this is just a setter and point to assignment page.**

### Field Patterns

When defining a field, you may optionally give it a [pattern](patterns.html) after the field's name. If provided, then the you will only be able to initialize or assign to the field using values that match that pattern.

    :::magpie
    defclass Point
        var x is Int
        var y is Int
    end

Here `x` and `y` are now constrained to number values by using `is Int` patterns. If you try to construct a `Point` using another type, or set a field using something other than an `Int`, you'll get an error.

### Immutable Fields

So far, we've seen fields defined using `var`, but you can also define them with `val`. Doing so creates an *immutable* field. Immutable fields can be initialized at construction time, but don't have a setter, so they can't be modified.

    :::magpie
    defclass ImmutablePoint
        val x is Int
        val y is Int
    end

    var point = ImmutablePoint new(x: 1, y: 2)
    point x = 2 // ERROR: There is no setter for "x".

### Field Initializers

Finally, when defining a field in a class, you can give it an initializer by having an `=` followed by an expression. If you do that, you won't need to pass in a value for the field when instantiating the class. Instead, it will automatically evaluate that initializer expression and set the field to the result.

    :::magpie
    defclass Point
        var x = 0
        var y = 0
    end

Here `Point`s will default to be `0, 0`. You can create a new one simply by doing:

    :::magpie
    var point = Point new()

## Inheritance 

**TODO**
