^title Messages
^index 5

Like any true-blue OOP language, the heart of Magpie is messages: things you tell objects to do. Almost everywhere you see an identifier like `foo`, you're seeing a message. Here's a simple example:

    :::magpie
    "some string" count

Here, we're sending a `count` message to a string object. Messages always follow the *receiver*, the object the message is being sent to. When an object receives a message, it may do some calculation (like `count` here, which will calculate the length of the string) or it may just return the value of a field:

    :::magpie
    Int name // gets the "name" field of the Int class

Messages are also used to look up methods. When you do a method call in Magpie, like:

    :::magpie
    list add("item")

Two operations are actually occurring here. First, we send the `add` message to `list`. That returns a reference to the method [function](functions.html) with `this` bound to `list`. Then we call that function, passing in `"item"`. In other words, like C++, Magpie has no special syntax for calling methods, it just uses messages to look them up and function application to call them.

#### Implicit Receiver

In Magpie, *all* names are message sends, but that leads to an infinite regress problem. What is the first message sent to? In a line of code like:

    :::magpie
    list count

If `list` is a message send too, what is the receiver? The answer is that Magpie allows implicit receivers. If a message doesn't have a receiver to the left of it, the interpreter will try to find an appropriate receiver to send the message to.

First, it will walk up the local scope chain. If there is a local variable with the name of the message, that variable's value will be returned. If it can't find a local variable with that name, it will send the message to `this`.

### Chaining Messages

Messages associate from left to right, so when you chain a series of them together, each subsequent message will be sent to the value returned by the previous one. In other words, this:

    :::magpie
    first second third

is equivalent to:

    var a = first
    var b = a second
    var c = c third

In C++/Java-style OOP syntax, that would be:

    first().second().third();

Magpie just ditches a lot of the punctuation.




### Regular Messages 

A regular message looks like:

    :::magpie
    list add("item")

This sends an `add` message to `list`, passing in `"item"`. You'll note that no `.` is needed between the receiver and the message.

A message name starts with a letter or underscore followed by any number of other letters, underscores, digits, or allowed punctuation characters. These are all valid message names in Magpie:

    :::magpie
    _   a   item1   punctuation?!   i$feel%FUNNY^*

Note that because punctuation characters are allowed in messages, it's important to use whitespace to separate things. (It also makes your code easier to read):

    :::magpie
    a+b   // the name "a+b"
    a + b // adds a and b

If a message takes no arguments, the `()` can and should be left off:

    :::magpie
    list count

If there are multiple arguments, they're separated with commas:

    :::magpie
    dictionary add("key", "value")

(Technically, all messages in Magpie take a single argument. In the above example, we're passing one argument to `add` a [tuple](compound-values.html) with fields "key" and "value". In practice, it works like you'd expect.)

#### Implicit Receiver

In many cases, you can leave off the receiver (the part to the left of the message name) in a regular message send:

    :::magpie
    add("item")

When you do this, Magpie will try to figure out what to send that message to. It
will first look in the local variables for a matching name, walking up the
nested scopes. So, if there was a local variable named `add`, it would call it
like a function.

If it's not found in local scope, it will try to send the message to `this`.
This works pretty much like other OOP languages. Note that because you can leave
off `()` for zero-argument messages, and because messages are sent to the local
scope first, that means named variables are actually just messages sent to local
scope.

### Operators

In Magpie, operators are just messages too:

    :::magpie
    1 + 2

That sends a `+` message to `1`, passing in `2`. Operators are distinguished from regular messages by their name. Where a regular message starts with a letter or underscore, an operator starts with a punctuation character. Otherwise, the naming rules for them are the same. These are valid operators:

    :::magpie
    +   -   /   *#$%   +something    *3

Unlike most languages (but like Smalltalk), Magpie does not have a fixed set of operators with detailed precedence and associativity tables. Instead, all operators have the same precedence and associate left to right. In other words, this in Magpie:

    :::magpie
    1 + 2 * 3

evaluates to *9* and not *7*. Parentheses are your friend here. This may take some getting used to, but I think the simplicity (no complicated operator precedence to remember) and flexibility (define your own operators for DSLs) are worth it.

### Chaining

Message sends associate from left to right, so when you chain a series of sends together, each subsequent message will be sent to the value returned by the previous one. In other words, this:

    :::magpie
    first second(arg) third fourth

is equivalent to:

    var a = first second(arg)
    var b = a third
    var c = b fourth

In C++/Java-style OOP syntax, that would be:

    first().second(arg).third().fourth();

Magpie just ditches a lot of the punctuation.

<p class="future">
I need to document getters and setters either here or under classes.
</p>
