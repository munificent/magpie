^title Calls

The yin and yang of Magpie's syntax are [messages](messages.html) and *calls*. Calls are the functional face of Magpie: they invoke functions or other function-like objects. For example:

    :::magpie
    print("Hello!")

Here we're calling the `print` function, and passing it the string `"Hello!"`. A call always has a *target* which appears on the left (here a message), and an *argument*. The argument is always placed inside parentheses.

The target can be any expression that evaluates to a function, or to a callable object. Consider this:

    :::magpie
    list add("item")

Here, we're calling the `add` method on a list, and passing in `"item"`. The target in this case is the entire expression `list add`, which sends an `add` message to `list`, returning the method to add an item in the list. We then call that function with `("item")`.

## Callables

The target of a call does not have to be a function. If the target isn't a function, Magpie will look for a `call` method on the object. If it can find one, it will invoke that instead. For example, you can get the character at a given index in a string like this:

    :::magpie
    "abcdefg"(2) // "c"

Since `"abcdefg"` isn't a function, that will be interpreted like:

    :::magpie
    "abcdefg" call(2)

Magpie doesn't have a distinct "array index" syntax for indexing into a collection, instead it just uses regular call syntax. To get an item from a list, you just do:

    :::magpie
    var items = Array of("apple", "banana", "cherry")
    items(1) // "banana"

You can thus define your own indexable classes just by defining a `call` method on them.
