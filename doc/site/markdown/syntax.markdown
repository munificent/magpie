^title Syntax
^index 2

Magpie's syntax is a good bit simpler than many other languages. Its main inspirations are [Io](http://www.iolanguage.com/) and [Ruby](http://www.ruby-lang.org/en/). Magpie tries to reduce the amount of required punctuation that encrusts code in other languages while at the same time being more flexible with what you are allowed to do.

Unlike most imperative languages, but like most functional languages, Magpie does not have *statements*, only *expressions*. Flow control, blocks, and variable declarations are all expression. This is valid in Magpie:

    :::magpie
    print(if result then "yes" else "no")

### Comments

Comments are as in C, C++, Java, etc.:

    :::magpie
    some code // this is a line comment
    // a line comment ends at the end of the line
    
    some more /* this is a block comment */ code code
    
    /* block comments
       can span multiple lines */

### Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // two expressions
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
wouldn't make sense. Specifically that means newlines following a comma (`,`),
colon (`:`), operator (`+`, `-`, etc.), or open brace (`(`, `[`, `{`) will be
discarded:

    :::magpie
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
    
    :::magpie
    0
    1234
    -5678

#### Strings

Strings are surrounded in double quotes:
    
    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
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

    :::magpie
    var a = { print("hi") }

That will create an expression object containing the code `print("hi")` and store a reference to it in `a`. It won't print anything.

#### Object Literals

An object literal builds a new object from scratch. Its class will be `Object`, and it will have the given fields defined on it. The syntax is:

    :::magpie
    var point = x: 1 y: 1
    // creates an object with fields "x" and "y"

Note that no separators are needed between the fields. The field names (followed by a colon) are enough to distinguish them.

### Messages

Like any true-blue OOP language, the heart of Magpie syntax is messages: things you tell objects to do. In Magpie almost everything is a message. A message looks like:

    :::magpie
    list add("item")

This sends an `add` message to `list`, passing in `"item"`. You'll note that no `.` is needed between the receiver and the message.

A message name starts with a letter or underscore followed by any number of other letters, underscores, digits, or allowed punctuation characters. These are all valid message names in Magpie:

    :::magpie
    _   a   item1   punctuation?!   i$feel%FUNNY^*

Note that because punctuation characters are allowed in messages, it's important to use whitespace to separate things. Also it makes your code easier to read:

    :::magpie
    a+b   // the name "a+b"
    a + b // adds a and b

If a message takes no arguments, the `()` can and should be left off:

    :::magpie
    list count

If there are multiple arguments, they're separated with commas:

    :::magpie
    dictionary add("key", "value")

#### Implicit Receiver

In many cases, you can leave off the receiver (the part to the left of the message name) in a message send:

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

In addition to regular named messages, the other common way to invoke a function or method is with an operator. In Magpie, operators are just messages too:

    :::magpie
    1 + 2

That sends a `+` message to `1`, passing in `2`. Operators are distinguished from regular messages by their name. Where a regular message starts with a letter or underscore, an operator starts with a punctuation character. Otherwise, the naming rules for them are the same. These are valid operators:

    :::magpie
    +   -   /   *#$%   +something    *3

Unlike most languages (but like Smalltalk), Magpie does not have a fixed set of operators with detailed precedence and associativity tables. Instead, all operators have the same precedence and associate left to right. In other words, this in Magpie:

    :::magpie
    1 + 2 * 3

evaluates to *9* and not *7*. Parentheses are your friend here. This may take some getting used to, but I think the simplicity (no complicated operator precedence to remember) and flexibility (define your own operators for DSLs) are worth it.

### Tuples

A series of expressions separated by commas creates a *tuple*:

    :::magpie
    1, 2, "three"  // creates a tuple of two ints and a string

If you aren't familiar with tuples, it may be a bit hard to wrap your head around them. A tuple is a *compound* value, but it isn't quite a *container*. A tuple doesn't *hold* values, it *is* values.

The simplest example is a point. A point in 2D space doesn't *hold* an X and a Y coordinate, it *is* one. You can't have a tuple that contains just one value&mdash; a single value is already a tuple (a mono-uple?) in the same way that a 1-dimensional point is just a number.

Tuples are a core part of Magpie. When you call a message with multiple arguments, you're actually passing a single one: a tuple.

### Functions

A language that claims to be functional needs to provide a simple syntax for  anonymous functions. Magpie uses the `fn` keyword for this. Here are some examples:

    :::magpie
    // single line:
    fn(a) print(a)
    fn(a, b) a + b
    
    // multiple line:
    fn(i)
        i = i * 2
        print(i)
    end

Anonymous functions can also have type annotations if you want. They follow the argument names:

    :::magpie
    fn(i Int) i * i
    fn(name String, age Int) print(name + " is " + age + " years old.")

Function expressions like this are just regular expressions. You can use them anywhere you like:

    :::magpie
    fn(outer)
        var a = fn(inner) fn(nested) print(inner + nested)
        takeFn(fn(a) a)
    end
    
    var ycombinator = fn(f) (fn(x) f(x(x))(fn(x) f(x(x))))

As you would expect, functions are
[closures](http://en.wikipedia.org/wiki/Closure_%28computer_science%29): they
can access variables defined outside of their scope.

### Variables

Variables in Magpie must be explicitly declared. This avoids a lot of annoying issues that can crop up with typoes in names and unintended variable scope without adding too much overhead. Variables are declared using `var`:

    :::magpie
    var a = 1 + 2

This creates a new variable `a` in the current scope and initializes it with the
result of the expression following the `=`.

Declaring a variable in an inner scope with the same name as an outer one is called *shadowing* and is not an error (although it's not something you likely intend to do much):

    var a = "outer"
    if true then
        var a = "inner"
        print(a) // prints inner
    end
    print(a) // prints outer

Declaring a variable with the same name in the *same* scope *is* an error:

    var a = "hi"
    var a = "again" // error!

#### Named Functions

You can create a function and assign it to a variable like any other value:

    :::magpie
    var double = fn(i) i * 2

But, since this is something you do frequently, Magpie has a shorter form that accomplishes the same thing:

    :::magpie
    var double(i) i * 2

### Assignment

**TODO**

* local vars
* assignment messages

### Blocks

If you want to evaluate several expressions where only a single one is expected, you can create a *block*. Many languages use curly braces (`{ }`) for blocks. In Magpie, a block starts with a newline and ends with `end` (or occasionally another keyword like `else`):

    :::magpie
    if happy? then print("I'm happy!") // no block
    
    if happy? then // <- a newline here starts the block
        print("I'm happy!")
        print("Really happy!")
    end // <- and this ends it

Blocks are allowed most places where an expression is expected. In fact, blocks *are* expressions: they evaluate to the last expression in the block:

    :::magpie
    var a =
        print("hi")
        3
    end

This will print "hi" and then define `a` with the value 3.

A block creates a nested local scope. Variables declared inside disappear when
the block ends (unless they're captured in a closure, of course). For example:

    :::magpie
    var a =
        var temp = 1 + 2
        temp * temp
    end

After evaluating that, `a` will be 6 and `temp` will no longer exist.

### TODO:

* flow control
* * if
* * let
* * while
* * for
* conjunctions
* callable
* precedence and associativity
