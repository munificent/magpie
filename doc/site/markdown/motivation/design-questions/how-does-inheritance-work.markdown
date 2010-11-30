How Does Inheritance Work?

Inheritance can be a nice feature, but it adds a *lot* of complexity to the language. In particular, all of these have to be addressed:

#### How are base classes initialized?

If a base class has fields that require values to be passed in to initialize them, the derived class needs some way to provide it. In Java, that's through `super()`, but it gets trickier in multiple inheritance where you need to identify *which* base class you're calling.

#### What happens if there are multiple paths to the same base class?

In other words, the [Deadly Diamond](http://en.wikipedia.org/wiki/Deadly_Diamond_of_Death). Say there are classes like:

      A
     / \
    B   C
     \ /
      D

A has a `foo` method which both B and C override. If you have an instance of D and call `foo`, which one gets called?

#### What happens if fields collide?

If A and B both have fields `foo` with different types, what happens when C tries to inherit from them both?

#### Can you inherit without subtyping?

C++ has private inheritance, which is essentially composition with less explicit forwarding. It basically copies all of the methods from the base class to the derived one, but does *not* set up a subtype relation where the derived class can be used where the base is expected. Should that be supported?

#### How are methods overridden?

Are all methods virtual? What happens if the types don't match?

### One Solution: No Inheritance

The simplest solution: no inheritance at all! If you want to share methods across classes, add the same method to multiple classes.

You can do "mixins" where you copy all of the methods from one class to another, but there's no implied long-term relationship between the two classes. (Actually, that's probably a terrible idea. It would be weird because if you add a method to a mixin class, classes that already had that mixin applied wouldn't get it. That means ordering of code would be important and fragile. Lame.)

To share code and state between classes, you use composition. Instead of inheriting from a base class, you have a field contain an instance of it. Then you forward method calls to it.

The downside, of course, is that inheritance is really handy. It makes it easy to share code between lots of classes. For some things, a subtype relation between concrete classes works well.

### Slightly Better: Explicit Delegation

A slightly looser approach is explicit delegation. We take the same approach that base classes are named fields on the derived class. This clears up a bunch of ambiguity and makes some problems trivial. For example, "calling the base class constructors" is really just initializing fields as usual.

Then on top of that, we add some simple support for automatic delegation. You can mark a field as a "delegate". Doing so means that if a member lookup fails on the object, it will cascade to trying to look it up on that delegate. If that fails, it proceeds to the next, etc. There needs to be a way to specify how delegates are ordered in case there are collisions where multiple delegates have a given member.

That gives you the simplicity of no inheritance with the convenience of not needing to manually forward. Code sharing is pretty easy to accomplish.

There are a couple of issues:

#### Is there subtyping?

If an object delegates to one of its fields, does that imply a subtype relation? If so, we'll have to do some checking to make sure that overridden methods (i.e. a member a delegate has that the primary object also has) have compatible types.

It might be interesting to actually answer "no" to this: no subtyping between concrete classes, even when delegating. That removes issues with override compatibility, I think. This is sort of Go's model: if you want subtype polymorphism, you use interfaces.

#### What is "this" in a delegated method?

This is the trickier question. If we pass a method on to a delegate, what is `this` within the body of that method? The implication is that it's just a straight forward, so `this` would be the object delegated to. For example:

    :::magpie
    class Foo
        def method() print(name)
        var name = "Foo"
    end
    
    class Bar
        delegate var foo = Foo new()
        var name = "Bar"
    end

    var bar = Bar new()
    bar method()

That would print "Foo" because when we invoke the delegated `method` method, it gets invoked on the object delegated to: `Foo`. This is good because it removes all of the confusion about overridden methods and other weird stuff. (For example, if `method()` was invoked with `this` set to the instance of `Bar`, we'd have to ensure that Bar's `name` getter was compatible with the one in Foo that `method()` was checked against. Each class is its own island.

The downside is that it means you can't mixin methods that act on the state of the original object. There's no way to define a method in Foo that can productively use state from Bar.

In theory that isn't much of a limitation: you should bundle the state with the methods that act on it. So Foo shouldn't need to act on Bar's state: that state should be pulled out of Bar and put into Foo. In practice, it's probably a pain.

### Bound Delegates

One way we could possible address that issue is by having the delegating object pass in itself to the delegate method as an additional parameter. Let's say in our original example, we really did want `method()` to print "Bar". That could be solved like this:

    :::magpie
    class Foo
        def method(receiver Bar) print(receiver name)
        var name = "Foo"
    end
    
    class Bar
        var foo = Foo new()
        var name = "Bar"
        
        def method()
            // Explicitly forward.
            foo method(this)
        end
    end

    var bar = Bar new()
    bar method()

The question now is how can we automate this using delegates? Can we make this
work:

    :::magpie
    class Foo
        def method(receiver Bar) print(receiver name)
        var name = "Foo"
    end
    
    class Bar
        delegate var foo = Foo new()
        var name = "Bar"
    end

    var bar = Bar new()
    bar method() // calls bar foo method(bar)

One tricky part is that member lookup is distinct from invokation. That means
we need to bind the receiver argument passed to the delegated method *before*
that method gets invoked. For example, given the above, this:

    :::magpie
    var bar = Bar new()
    var m = bar method

For this to work as expected, `m` here needs to be a reference to Foo's `method` where the argument has already been bound to `bar`. So it looks like we'll need something like currying and partial application. Consider:

    :::magpie
    class Foo
        def greet(receiver Bar) fn (name String)
            print(receiver name + " greets " + name)
        end
    end
    
    class Bar
        var name = "Bar"
        delegate var foo = Foo new()
    end
    
    var bar = Bar new()
    bar greet("Bob") // prints "Bar greets Bob"

In this way, resolving a delegated method is:

1. Look for a method with the right name on the delegated object.
2. If found, *immediately invoke it, passing in the delegating object*.
3. Return the result of that.

This is a little fishy because it means *all* delegated methods need to be curried like this, which is cumbersome. Any class will have to be designed to be used as a delegate or not, and can't easily do double-duty.

What this gives us is that inside the delegated method, we have access to all of the state we want: `this` will be the delegate object so you can get to its state, and we'll define a variable in the surrounding closure whose value is the original receiver.

That does raise the question of what happens with indirect delegation. If Foo delegates to Bar which delegates to Bang, we'll have a reference to Bang (`this`) and either Foo or Bar (the closure), but not both. Is that OK? Which should it be? If it's Foo, we won't know how to type-check the delegate method.




