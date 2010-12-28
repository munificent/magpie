^title Objects
^index 2

*Objects* are the data that Magpie programs operate on. They are the nouns of
the language. An address or a phone number is an object, as is a spaceship in a
game, or an HTTP request in a web app. Magpie is a "pure" object-oriented language in that *all* values are represented by objects. Numbers, strings, and boolean values are objects, as are functions, classes, chunks of code, the special value `nothing`, and even types. The whole kit and caboodle.

Every object in Magpie is an instance of some *class*. The class determines what
data an object contains or represents, and what operations the object can 
perform. Magpie has several built-in classes that deserve special attention and
that you'll use to build your own classes. They are:

1. [Booleans](objects/booleans.html)
1. [Numbers](objects/numbers.html)
1. [Strings](objects/strings.html)
1. [Nothing](objects/nothing.html)
1. [Tuples](objects/tuples.html)
1. [Records](objects/records.html)
1. [Expression Objects](objects/expression-objects.html)

Functions are objects too, and could go here, but we'll break them out into 
their own section.
