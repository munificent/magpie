^title Messages
^index 5

Like any true-blue OOP language, the heart of Magpie is messages: things you tell objects to do. In Magpie almost everything is a message. There are two flavors of messages in Magpie: regular and operators.

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
I need to document callables, getters and setters here.
</p>
