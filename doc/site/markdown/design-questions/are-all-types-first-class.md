^title Are All Types First-Class?

In other words, can you have an object in Magpie that represents the type `Int |
Bool`?

Answer: **Yes.**

Things like generics will need to internally store their type parameters. For
example:

    :::magpie
    class List[E]
        items E[]
    end

That type parameter should be useful at the type level, for things like:

    :::magpie
    def List[E] add(item E)
        // ...
    end

But should also be directly available in the same way that you can do `typeof(T)`
in C#:

    :::magpie
    def List[E] displayItemType()
        print(E string)
    end
    
This is fine if you only instantiate generics with class type arguments. But
it's perfectly valid to also do:

    :::magpie
    var optionalInts = List[Int | Nothing] new
    
Which implies that `Int | Nothing` must itself resolve to a first-class object
in Magpie.

This is good because (as of 8/19) that's the current path the implementation is
taking. It just makes some stuff harder because type checking has to bounce
between Java and Magpie more than we'd like.

This is also good in that it follows the goal of making everything the interpreter
knows also available to Magpie.