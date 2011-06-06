^title Objects

*Objects* are the data that Magpie programs operate on. They are the nouns of
the language. An address or a phone number is an object, as is a spaceship in a
game, or an HTTP request in a web app. Magpie is a "pure" object-oriented language in that *all* values are represented by objects. Numbers, strings, and boolean values are objects, as are functions, classes, chunks of code, the special value `nothing`, and even types. The whole kit and caboodle.

Every object in Magpie is an instance of some [class](classes.html). Magpie has
several built-in classes that deserve special attention and that you'll use to
as the building blocks for your own classes. They are:

1. [Primitives](primitives.html)
1. [Arrays](arrays.html)
1. [Records](records.html)
1. [Functions](functions.html)
1. [Quotations](quotations.html)

While the built-in types are all different, they have one important thing in common: they are all *immutable*. Once created, they cannot be modified. This is true of things like numbers, of course, but also even strings and arrays. (If you want a mutable array-like object, you'll use a list.)

Immutability may seem like a limitation (and strictly speaking it is), but it has some nice benefits. First, it can make your code easier to understand. If you pass a string or a record to some [method](multimethods.html), you don't have to worry about it changing it on you. More importantly, immutable objects are inherently *concurrency-friendly:* you can share them between threads without having to worry about synchronization or locking.