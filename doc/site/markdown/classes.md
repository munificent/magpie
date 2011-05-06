^title Classes

**TODO: This is out of date now that multimethods are in.**

Magpie is a class-based language. That means everything you can stick in a
variable will be an *object* and every object is an instance of some *class*.
Even [primitive types](primitives.html) like numbers and booleans are full-featured objects.

Objects exist to *store data*, package it together, and let you pass it around.
Objects also have *identity*&mdash; you can tell two objects apart even if they
contain the same data. But most importantly, they are the receivers for
[messages](expressions/messages.html). You write programs that do stuff in
Magpie by telling objects to do things.

Classes define the operations an object supports and the kinds of data it can
store. Even though you send a message to a specific *instance*, it's the
object's *class* that determines how it will behave. Even though each instance
stores data, it's the class that determines which named
[fields](classes/fields.html) the object has. Fields, methods, and the other
attributes that a class defines for a set of objects are collectively known as
*members*.

Summarized, a class defines the set of members that all objects of that class
share.

## Classes and Metaclasses

Just about everything in Magpie is an object, and that includes classes. Classes are first-class objects that you can put into variables, pass around, and send messages. Much like Ruby or Smalltalk, you construct new objects simply by sending a `new` message to the appropriate class.

This raises the question, "What's the class of a class object?" The answer is a *metaclass*. Each class object (for example, `Int`, the class of integers) is an instance of a metaclass (in this case `IntClass`). Each metaclass will only have one instance: the class that it describes. When you send a message to a class, like `Int parse("123")`, that method is defined in the class's metaclass.

(This of course raises the further question of what the class of a metaclass object is. It's just `Metaclass`, the class of metaclasses. There's no need to have distinct metametaclasses because there are no shared methods on metaclass objects.)

## Imperative and Declarative Syntax

Statically-typed languages like Java and C++ have a declarative syntax for defining classes. These definitions aren't "executed" at runtime, they just define the class *a priori*. When your Java program starts up the classes are magically already there.

Dynamically-typed languages like Ruby and Python have a more imperative system
for defining classes (even though the syntax is still often declarative). A
class definition is simply a chunk of code that gets executed at runtime when a
script is evaluated.

Both have their advantages. Declarative class syntax is generally easier to read and reason about and makes things like mututally recursion and circular references less tricky. On the other hand, imperative class creation makes it easier to do dynamic things like extend an existing class, or add a method whose name is determined at runtime. Ruby's Active Record pattern would be impossible with a static class declaration system.

Magpie believes different problems have different solutions. It has a core imperative API for building classes procedurally, then it layers a nice declarative syntax that the parser translates that down to. Most of the time, you'll work at the declarative level:

    :::magpie
    class Friend
        def introduce() print("I'm " ~ name ~ " and I love " ~ food)

        var name
        var food
    end

But when you need the flexibility, you can go under that:

    :::magpie
    var Friend = Class new("Friend")
    Friend defineMethod("introduce",
        fn() print("I'm " ~ name ~ " and I love " ~ food)
    end)
    Friend declareField("name")
    Friend declareField("food")

This guide will usually show examples using the declarative syntax because that's what you'll use the most, but understand that that's just a veneer over the core API.