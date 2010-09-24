^title Syntax

Magpie's syntax is a good bit simpler than many other languages. Its main inspirations are [Io](http://www.iolanguage.com/) and [Ruby](http://www.ruby-lang.org/en/). Magpie tries to reduce the amount of required punctuation that encrusts code in other languages while at the same time being more flexible with what you are allowed to do.

### Comments

Comments are as in C, C++, Java, etc.:

    some code // this is a line comment
    // a line comment ends at the end of the line
    
    some more /* this is a block comment */ code code
    
    /* block comments
       can span multiple lines */

### Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    // two expressions
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
wouldn't make sense. Specifically that means newlines following a comma (`,`),
colon (`:`), operator (`+`, `-`, etc.), or open brace (`(`, `[`, `{`) will be
discarded:

    var a = 1,
            2 // a will be the tuple (1, 2)
    
    var b = 1 + 
            2 // b will be 3

    print(
        "hi") // prints "hi"

### Literals

Literals are the atomic building blocks of a language. Magpie currently doesn't have a very wide set of them, but it's getting there. It supports:

#### Booleans

A boolean value (type `Bool`) can be `true` or `false`.

#### Numbers

Magpie doesn't have floating point numbers yet. (I know, I know. I'm getting there.) Integers look like you expect:
    
    0
    1234
    -5678

#### Strings

Strings are surrounded in double quotes:
    
    "hi there"

A couple of escape characters are supported:

    "\n" // newline
    "\"" // a quote
    "\\" // a backslash

#### Nothing

Magpie has a special type `Nothing` that has only one value `nothing`. (Note the difference in case.) It functions a bit like `void` in some languages: it indicates the absence of a value. A function like `print` that doesn't return anything actually returns `nothing`.

It's also similar to `null` in some ways, but it doesn't have [the
problems](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-nothing/)
that `null` has in most other languages. It's rare that you'll actually need to
write `nothing` in code. Most of the time the interpreter will infer it or let
you omit it, but it's there if you need it.

#### Expression Literals

An expression literal is a chunk of code that isn't evaluated. Instead, it's just bundled up into a data structure that you can pass around. You can't do much with them yet, but they will eventually be used for metaprogramming. To create an expression literal, just enclose any expression in curly braces:

    var a = { print("hi") }

That will create an expression object containing the code `print("hi")` and store a reference to it in `a`. It won't print anything.

#### Object Literals

An object literal builds a new object from scratch. Its class will be `Object`, and it will have the given fields defined on it. The syntax is:

    var point = x: 1 y: 1
    // creates an object with fields "x" and "y"

Note that no separators are needed between the fields. The field names (followed by a colon) are enough to distinguish them.

### Messages

Like any true-blue OOP language, the heart of Magpie syntax is messages: things you tell objects to do. In Magpie almost everything is a message, even variable names. Even things that look nothing at all like messages are often just a little syntactic sugar on vanilla message sends.

There are two main flavors of messages: regular messages and operators.

#### Regular Messages

A regular message looks like this:

    list add("item")

This sends an `add` message to `list`, passing in `"item"`. You'll note that no `.` is needed between the receiver and the message.

A regular message name starts with a letter or underscore followed by any number of other letters, underscores, digits, or allowed punctuation characters. These are all valid message names in Magpie:

    _   a   item1   punctuation?!   i$feel%FUNNY^*

Note that because punctuation characters are allowed in messages, it's important to use whitespace to separate things. Also it makes your code easier to read:

    a+b   // the name "a+b"
    a + b // adds a and b

If a message takes no arguments, the `()` can and should be left off:

    list count

If there are multiple arguments, they're separated with commas:

    dictionary add("key", "value")

#### Operators

The other common kind of messages are operator messages. They look like:

    1 + 2

That sends a `+` message to `1`, passing in `2`. Operators are distinguished from regular messages by their name. Where a regular message starts with a letter or underscore, an operator starts with a punctuation character. Otherwise, the naming rules for them are the same. These are valid operators:

    +   -   /   *#$%   +something    *3

Unlike most languages (but like Smalltalk), Magpie does not have a fixed set of operators with detailed precedence and associativity tables. Instead, all operators have the same precedence and associate left to right. In other words, this in Magpie:

    1 + 2 * 3

evaluates to *9* and not *7*. Parentheses are your friend here. This may take some getting used to, but I think the simplicity (no complicated operator precedence to remember) and flexibility (define your own operators for DSLs) are worth it.

### Tuples

A series of expressions separated by commas creates a *tuple*:

    1, 2, "three"  // creates a tuple of two ints and a string

If you aren't familiar with tuples, it may be a bit hard to wrap your head around them. A tuple is a *compound* value, but it isn't quite a *container*. A tuple doesn't *hold* values, it *is* values.

The simplest example is a point. A point in 2D space doesn't *hold* an X and a Y coordinate, it *is* one. You can't have a tuple that contains just one value&mdash; a single value is already a tuple (a mono-uple?) in the same way that a 1-dimensional point is just a number.

Tuples are a core part of Magpie. When you call a message with multiple arguments, you're actually passing a single one: a tuple.

### Functions

A language that claims to be functional needs to provide a simple syntax for  anonymous functions.

### TODO:

* functions
* flow control
* * if
* * let
* * while
* * for
* blocks
* variables
* * declaration
* * assignment
* conjunctions
* callable
* precedence and associativity
