^title Messages
^index 5

Like any true-blue OOP language, the heart of Magpie is *messages*: things you tell objects to do. Almost everywhere you see an identifier like `foo`, you're seeing a message. Here's a simple example:

    :::magpie
    "some string" count

Here, we're sending a `count` message to a string object. Messages always follow the *receiver*, the object the message is being sent to. When an object receives a message, it may do some calculation (like `count` here, which will calculate the length of the string) or it may just return the value of a field:

    :::magpie
    var point = x: 1 y: 2
    point x // Gets the "x" field of the point record.

Messages are also used to look up methods. When you do a method call in Magpie, like:

    :::magpie
    list add("item")

Two operations are actually occurring here. First, we send the `add` message to `list`. That returns a reference to the method [function](functions.html) with `this` bound to `list`. Then we call that function, passing in `"item"`. In other words, like C++, Magpie has no special syntax for calling methods, it just uses messages to look them up and function application to call them.

It also means that you can grab a reference to a method and invoke it later. This is perfectly valid:

    :::magpie
    var adder = list add // Get the method.
    adder("item") // Call it.

#### Implicit Receiver

In Magpie, *all* names are message sends, but that leads to an infinite regress problem. What is the first message sent to? In a line of code like:

    :::magpie
    list count

If `list` is a message send too, what is the receiver? The answer is that Magpie supports *implicit receivers*. If a message doesn't have a receiver to the left of it, the interpreter will try to find an appropriate receiver to send the message to.

First, it will walk up the local scope chain. If there is a local variable with the name of the message, that variable's value will be returned. If it can't find a local variable with that name, it will send the message to `this`.

### Chaining Messages

Messages associate from left to right, so when you chain a series of them together, each subsequent message is sent to the value returned by the previous one. In other words, this:

    :::magpie
    first second third

is equivalent to:

    :::magpie
    var a = first
    var b = a second
    var c = b third

In C++/Java-style OOP syntax, that would be:

    :::magpie
    first().second().third();

Magpie just ditches a lot of the punctuation.

<p class="future">
I need to document getters and setters either here or under classes.
</p>
