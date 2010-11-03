^title How are Classes Declared?

Magpie's core system for defining classes is imperative and just uses regular message sends. But on top of that is a declarative sugar to make things cleaner.

Coming up with a good syntax for that is surprisingly hard. There are a number of different kinds of members users need to be able to define, and some corner cases where it's hard to distinguish them. Balancing that against terseness and minimizing keywords is tricky.

A complete sample that shows everything is:

    :::magpie
    class Example
        // define ctor
        this(x Int)
            doSomething()
        end
    
        // define instance field
        var foo Int = 123
        
        // define instance method
        def foo(arg Int -> Bool) = body
    
        // define instance getter
        get foo Int = 123
    
        // define instance setter
        set foo Int = print(it)
    
        // declare instance field
        var foo Int
    
        // declare instance method
        def foo(arg Int -> Bool)
    
        // declare instance getter
        get foo Int
    
        // declare instance setter
        set foo Int
        
        // define shared field
        shared var foo Int = 123
    
        // define shared method
        shared def foo(arg Int -> Bool) = body
    
        // define shared getter
        shared get foo Int = 123
    
        // define shared setter
        shared set foo Int = print(it)
    end
    
    interface Example
        // declare instance method
        def foo(arg Int -> Bool)
    
        // declare instance getter
        get foo Int
    
        // declare instance setter
        set foo Int
    end

### Why have separate keywords for fields and methods?

We need to be able to distinguish a method from a field of tuple or function type. Consider:

    :::magpie
    foo(a, b)

Is that a field "foo" whose type is a tuple of types "a" and "b", or a method that takes two dynamic arguments? Or:

    :::magpie
    foo(a -> b)

Is that a method that takes a dynamic argument and returns a value of type "b", or a field whose type is a function from "a" to "b"? To clarify this, we need some explicit keyword or keywords to distinguish methods from fields.

### Why have a keyword for methods?

If there are keywords for the other types of members, we could infer no keyword at all to mean method (since methods are likely the most common member). That would make things more terse. The downside is that it looks less consistent. Consider:

    interface Type
        canAssignFrom(other Type -> Bool)
        getMemberType(name String -> Type | Nothing)
        getSetterType(name String -> Type | Nothing)
        get string String
        get type Type
        ==(other -> Bool)
        !=(other -> Bool)
    end

The getters look really out of place there. So, for now, we'll try keywords for everything and see how it goes. As a nice bonus, this means that we can support calling arbitrary methods inside a class body, since they are not ambiguous with method declaration. So we can make things like this work:

    class Foo
        mixin(Bar) // call a method on Foo
    end

### Why "get" and "set"?

We need some keyword to distinguish getters and setters from regular methods or fields. Otherwise it becomes hard to parse without backtracking. "get" and "set" aren't great keywords: they're simple verbs that would ideally be usable by end users. But they're conveniently three letters which lines up nicely with "var" and "def", and I don't have any better ideas.

### Why "=" after the type in a method definition?

We need to be able to distinguish a method declaration (no body), from a method definition whose body is a block. Without some special syntax, in both cases there would be a newline immediately after the type declaration, so there would be no way to tell the two apart. Using `=` makes it clear that the definition follows.

Another option would be a specific `abstract` keyword or something for method declarations. That would be more terse for the common case (defined methods), but it would make interface definitions kind of cumbersome. Using `=` instead is consistent with how fields are declared/defined.




