^title How Do Fields Work?

An object is a bundle of state and a set of methods. Classes generally handle the "set of methods" part. How does state work? There are two common paths that I'll call "struct" and "scope". Javascript is the struct approach: an object's state is just public fields accessed from this:

    :::javascript
    // define some state:
    this.foo = bar;
    
    // access it:
    this.foo;

Smalltalk, Ruby and Finch take the "scope" approach: inside a method, variables with a certain name will be looked up in the object's dictionary:

    :::finch
    Obj :: method {
        // define some state
        _foo = bar
        
        // access it
        _foo
    }

Each has its pros and cons:

### The Struct Path

This encourages users to think of an object as a dictionary of named properties. Its state is implicitly public and open to easy external modification. Objects feel open and flat. It has this going for it:

*   Public-by-default fields make it easy to build ad-hoc objects and property 
    bags.
*   Easy to add or modify state outside of an object's methods.
*   Don't have to write getter/setter wrappers.
*   Lines up with the current implementation (as of 10/05/2010).
*   Conceptually unifies state and methods: state is always accessed through 
    methods.

### The Scope Path

This encourages users to think of state as fundamentally separate from methods and fully encapsulated within an object. Object's feel sealed and self-contained. It's good for:

*   Private-by-default fields make it easy to build safely encapsulated objects.
*   Conceptually unifies state and local variables: state is just a special kind 
    of variable.
*   Encourages a simple syntax for defining fields:
    
        :::magpie
        var _field = 123

### The Answer

Right now, I'm leaning towards the scope approach. One of the tedious parts of
Finch is that fields require explicit getters and setters. We could solve that
in a couple of ways. One way would be to get the existing field
declaration/definition support and have that create a field and matching getter
and setter too. It would be nice to allow readonly or writeonly variations too.

The other question this brings up is how is state type-checked? Beyond the basic limitation that variables cannot change type, I'm consider not doing any type-checking for fields. The type-checker at that point would essentially operate at the unit test level: it would check methods but you couldn't explicitly declare field types.

It might be interesting to try that and see how it feels.
