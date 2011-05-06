^title How Do Callable Fields Work with Multimethods?

Consider:

    :::magpie
    class Foo
        var bar = fn(i) print("field")
        def bar(i) print("method")
    end

    var foo = Foo new()
    foo bar(123)

The last line can be interpreted two ways:

1.  Look up `bar`, then invoke the result.
2.  Call the `bar()` method, passing in `123`.

Both are valid use cases, how can they be distinguished? In the current system, it would always be given the second interpretation. If the class is like:

    :::magpie
    class Foo
        var bar = fn(i) print("field")
    end

When you try to do:

    :::magpie
    Foo new() bar(123)

It will simply fail to find a method, since it's looking for one specialized to `(this Foo, i Int)` and all that exists is the field getter: `(this Foo)`. What to do?

## Option 1: No Callables

The Smalltalk/Io/Ruby solution is to not have functions. An expression like `foo bar(123)` always means "call the method `bar` passing in `123`" because there are no directly invocable objects. If `bar` was a field holding a callable, you'd have to do:

    :::magpie
    foo bar call(123)

This solves the problem, but it's kind of ugly. Multimethods are a step *towards* being more functional, so it seems weird for the syntax to step *back*.

## Option 2: Fallback Dispatch

Maybe we could just try both interpretations in order. So if you do:

    :::magpie
    foo bar(123)

First it will try to find a method specialized to `(this Foo, arg Int)`. If it finds that, good. If not, it then tries to find `(this Foo)`. If that succeeds, it then tries to immediately invoke the result passing in `123`.

That would work, I think, but it feels ugly.