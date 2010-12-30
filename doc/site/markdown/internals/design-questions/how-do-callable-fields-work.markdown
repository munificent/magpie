^title How Do Callable Fields Work?

In other words, how do we distinguish between a method that takes an argument,
and a method that doesn't take an argument but returns a callable object?

The scenario is this:

    class Foo
        this() this items = Array of(123)
        bar() items(123)
    end

The body of `bar` method could be interpreted to mean two different things:

1. Invoke the `items` method, passing in `123`.
2. Look up the field `items` (which is done using a getter method that takes no
   argument) and then call `call` on it.

The second is the intended interpretation in this case. Unfortunately, a
strictly Io-style syntax cannot support that. (Io doesn't have callables. You
always invoke a named method. Getting an item from a list is `list at(123)`.)

The way Python and C# handle this is with properties. A property is *not* a
method, and the distinction is known at runtime. This means we can disambiguate
the two scenarios above like this:

    look up the member "items"
    if it's a property
        invoke the property (with no argument, of course)
        send a "call" message to the result, passing in the argument (123 here)
    else
        invoke the method
    end

Before, we tried to treat all zero-argument methods implicitly as properties.
Unfortunately, we'll have to make an interpreter-visible distinction so that it
can behave differently depending on whether or not a member is a property.

While having explicit support for properties feels a bit gross, it does make
some things more consistent. Before this, implicitly calling a callable did work
if it was in a local variable or a dynamic (not declared and hence wrapped in a
getter) field. That's because the interpreter knew it was looking at a local
variable or field and therefore it couldn't be a method invocation, so it would
implicitly add the call. Now it can apply the same logic to declared fields and
other getters.

On the user side, there are two questions:

1. How do I declare a property?
2. Which things should be zero-argument methods, and which should be properties?

Scala's answer to 2 is that functions with side-effects should have an explicit
`()` and others should not. Io assumes that all zero-argument functions should
omit the `()`. I lean towards that simply because it's shorter. Magpie is
different enough that user's shouldn't expect a `()`-less method to always be
"field-like".

So the answer to 2 is "all zero-argument methods should be properties". Which
lets us neatly answer 1 by automatically creating a property if the function has
no named parameters.

This lets us come full-circle and actually eliminate properties as something tangible the user needs to think about. Instead, we get back to our original scenario. The way we disambiguate it is:

    look up the method "items"
    if it has no named parameters
        invoke it with no argument
        send a "call" message to the result, passing in the argument (123 here)
    else
        invoke the method with the argument
    end
