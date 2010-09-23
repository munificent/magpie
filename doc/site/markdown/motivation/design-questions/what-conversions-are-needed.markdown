^title What Conversions Are Needed?

In other words, what kinds of type-casts and conversions does Magpie need?

First, we'll need to clarify some terms. A "cast" is a procedure that changes the type of an object as seen by the type-checker. No runtime transformation is involved. A "conversion" is a procedure that actually produces a new object. 

Magpie doesn't have anything like the conversions of a static language. Things that do conversion are just regular methods with distinct names like "parse" (convert a string to some other form).

So, casts. What kinds of casts does Magpie need to support, and what do they look like? Before we get into use cases, there's one important point to keep in mind:

### Static evaluation

We need to be able to cast to any type, not just simple named ones like classes
and interfaces. That means casting operations need to accept full type
expressions that are evaluated at type-check time. That, unfortunately, means we
*can't* make casts just be methods on types, because the type-checker would have
a very hard time being smart enough to understand that. Consider:

    var a = Foo cast(bar)

The intent here is that a will hold a value `bar`, statically typed to be `Foo`. We can do this by simply defining the return type of `Foo cast()` to be `Foo`. That's easy. Now what about this:

    var a = (Foo | Bar) cast(bar)

The type-checker will look at `Foo`, and find its `|` method. From there it will get the return type of that and look up the `cast` method on that type. For this to work correctly then, the static type of `Foo |()` would need to evaluate the argument *value* passed to it. No dice.

This implies that casting operations, at least ones general enough to apply to arbitrary type expressions (which include or types, arrays, generics, etc.), will need to use static arguments (see [Where Do Type Expressions Appear?](where-do-type-expressions-appear.html]).

### Use cases

Back to the actual use cases for casting. There are a few use cases I can think of:

#### 1. An upcast

This widens the inferred type of an expression. By nature it will always
succeed. The main use case for this is since we don't allow variables to change
type (which is necessary to address another issue), it may be important to have
a variable's type be a supertype of what's directly inferred from its
initializer. For example:

    var a = 123
    a = "hi" // error

But if we could widen `a`'s type at the point of initialization to allow
`String`s, that would be fine:

    var a = 123 upcast[Int | String]
    a = "hi" // ok now, no type change

If widening variable types is the only use case for upcasts, we could consider rolling this into variable declaration syntax:

    var a Int | String = 123

There is at least one other use case, though: field initializers. For now, let's leave off any special syntax since this should hopefully not be needed frequently anyway.

#### 2. An unsafe cast

In the implementation of certain low-level type-related methods, or in other weird places, you may just need to straight up lie to the type-checker. For example, if you've deserialized an object and are sure that it does match a certain type, it's valid to just do an unsafe cast to tell the type-checker what you already know.

The syntax for this should highlight its unsafety. Something like:

    var a = foo unsafeCast![Bar]

#### 3. An asserted downcast

This is the common case where you're downcasting an object and you're quite certain the cast will succeed. Although I hope to minimize places where you need this, there's still cases where you get an `Object` but you know it's *really* a `Foo`.

The desired behavior is "check the type and if it isn't what I expect, blow up". More specifically, it will likely throw an exception. This is what doing a cast in a C# or Java does. In Magpie:

    var a = someObj cast[Foo | Bar]

#### 4. A potential downcast

Finally, the most common case: a downcast where you're not sure if it will
succeed or not. The desired behavior is "if it's this type, do this". More
specifically, it will return a value whose type is the desired type or
`Nothing`. At runtime, it will do the type test and return the original value on
success and nothing on failure. This plays nicely with `let`:

    let b = a as[Foo] then
        ...
    end

#### 5. A Duck-typed cast

There's one other use case I've thought of. In situations like mocking or testing, it's sometimes nice to be able to pass in an object as some type when the actual object is an unrelated type. `as` and `cast` will use nominal rules for compatibility (i.e. the object must be in the class hierarchy to succeed). 

For mocking, it would be nice to have looser structural compatibility. This basically lets you duck type any type and takes advantage of Magpie's dynamic core. We'll call this "masquerading". An object of one concrete type can masquerade as another unrelated type as long as its methods are compatible. It looks like:

    var a = someObj masqueradeAs[Foo] // returns Foo | Nothing
    var b = someObj masqueradeCast[Foo] // returns Foo
