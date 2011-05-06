^title Can All Generic Arguments Be Inferred?

Generics without inference is a real pain for many cases. Consider:

    :::magpie
    def max[T](a T, b T -> T)
        if a > b then a else b
    end

It's a real chore to have to call that like:

    :::magpie
    max[Int](2, 4)

The only reason we even need to make `max` generic is so that the return type
matches the arguments.

This is especially egregious in an optionally-typed language. At runtime, we're
doing work there to instantiate the function when there's no benefit since the
types are ignored at that point anyway.

One way around this is to infer type arguments from value arguments. So the
above becomes:

    :::magpie
    def max(a 'T, b 'T -> 'T)

With that, you just do:

    :::magpie
    max(2, 3)

At runtime, the types are all ignored and it just becomes a regular dynamic
function. At check time, we infer the return type from the types of the 
arguments. Everyone is happy.

## Explicit Type Arguments

But there's a problem. In static languages, there are plenty of places where you
call a generic method (or constructor) with no value arguments, *just* type
arguments. For example, consider a service locator that caches objects by their
type:

    :::csharp
    class Locator {
        T Locate<T>() {
            // look up instance of T in dictionary...
        }
    }

That would translate to something like this in Magpie:

    :::magpie
    class Locator
        def locate[T](-> T)
            // ...
        end
    end

There is no runtime argument to `locate`, so we can't infer the type. We could
hope to pass in the located type as a value parameter, like so:

    :::magpie
    class Locator
        def locate(type Type -> ???)
            // ...
        end
    end

But then the return type of the function won't be known at type-check time. Bad.
From this reasoning, I went ahead with Magpie's current generics system where
generic functions are special functions evaluated at check time, and there's no
inference. It works, but it's lame.

## A Curious Solution

But, if we could infer type arguments like in the `max` example, there may even
be a solution for our service locator one. What if it looked like this:

    :::magpie
    class Locator
        def locate(type Type['T] -> 'T)
            // ...
        end
    end

Let's say we call it like this:

    :::magpie
    var service = myLocator locate(MyService)

At runtime, of course, this works fine. What does the type-checker see?

It will see a call to `locate`, and know the type of its argument. In this case,
it will be `MyServiceClass`, the metaclass of the `MyService` class, and the 
type of the class object.

If we make each metaclass an instance of a generic class, whose type argument is 
the class that the metaclass is... meta-ing?... then the type-checker can
reconstruct back *down* to that class given the metaclass.

So the type-checker will see `MyServiceClass` where it expects `Type['T]`. It
asks that metaclass, "are you an instance of `Type['T']`, and if so, what is 
`T`?" and the metaclass should be able to give that back.

To make that work, we'll have to implement something like the `inferTypesFrom`
stuff I described in the [first generics design question](how-are-generic-functions-checked.html). But, now, given that, we
should be able to get rid of the old `[]`-style generic stuff completely. 
Inference for everything!

