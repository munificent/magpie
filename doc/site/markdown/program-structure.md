^title Program Structure

Magpie programs are stored in plain text files with a <tt>.mag</tt> file extension. Magpie does not compile ahead of time: programs are interpreted directly from source, from top to bottom like a typical scripting language.

## Comments

Comments are as in C, C++, Java, etc.:

    :::magpie
    some code // This is a line comment.
    // A line comment ends at the end of the line.

    some more /* This is a block comment. */ code code

    /* Block comments
       can span multiple lines. */

Unlike those languages, block comments nest in Magpie. That's handy for commenting out chunks of code which may themselves contain block comments.

    :::magpie
    code /* A /* nested */ block comment */ code code

## Doc Comments

In addition to regular line and block comments, Magpie has a third kind of comment called *documentation comments* or simply *doc comments*. They start with three slashes and proceed to the end of the line.

    :::magpie
    def square(n is Int)
        /// Returns `n` squared.
        n * n
    end

Doc comments are used to document entire constructs: [modules](modules.html), [classes](classes.html), [methods](multimethods.html), etc. Unlike other comments, doc comments are *not* ignored by the language. This means they are only allowed where they are expected: at the beginning of a file, method body, or class definition:

    :::magpie
    defclass Address
        /// A postal address.
        val street
        val city
        val state
    end

Doc comments are formatted using [Markdown](http://daringfireball.net/projects/markdown/) and are intended to be parsed to generate documentation files.

## Reserved Words

Some people like to see all of the reserved words in a programming language in one lump. If you're one of those folks, here you go:

    :::magpie
    and async break case catch def defclass do end else
    false fn for if import in is match not nothing or
    return then throw true val var while xor

Also, the following are *punctuators* in Magpie which means they are both
reserved words and they can be used to separate tokens:

    :::magpie
    ( ) [ ] { } , . .. ...

The only built-in operator is `=`. All other operators are just methods, as explained below.

## Names

Identifiers are similar to other programming languages. They start with a letter or underscore and may contain letters, digits, and underscores. Case is sensitive.

    :::magpie
    hi
    camelCase
    PascalCase
    _under_score
    abc123
    ALL_CAPS

## Operators

Magpie does not have many built-in operators. Instead, most are just [methods](multimethods.html) like any other method. However, the grammar of the language does treat them a bit specially.

Lexically, an operator is any sequence of punctuation characters from the following set:

    :::magpie
    ~ ! $ % ^ & * - = + | / ? < >

Also, the special tokens `..` and `...` are valid operator names. But a `=` by itself is not&mdash;that's reserved for [assignment](variables.html#assignment).

<p class="future">
The exact set of operator characters is still a bit in flux.
</p>

These are all valid operators:

    :::magpie
    +
    -
    *
    ?!
    <=>&^?!

When expressions are parsed, infix operators have the same precedence that you expect from other languages. From lowest to highest:

    :::magpie
    = !
    < >
    .. ...
    + -
    * / %

Every operator on the same line above has the same precedence. If an operator has multiple characters, the first determines the precedence. So this (unreadable) expression:

    :::magpie
    a +* b *- c <!! d !> e %< f

Will be parsed like:

    :::magpie
    (((a +* (b *- c)) <!! d) !> (e %< f))

The goal here is to have code that works more or less like you expect coming from other languages while still being a little more open-ended than those languages.

## Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // Two expressions:
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
doesn't make sense. Specifically, that means newlines following a comma (`,`), equals (`=`), backtick (<code>\`</code>), or infix operator (`+`, `-`, etc.) will be discarded:

    :::magpie
    val a = 1,
            2 // a will be the record (1, 2).

    val b = 1 +
            2 // b will be 3.

    val c = true and
            false // c will be false.

If you specifically want to ignore a newline where it otherwise *would* separate two expressions, you can end the line with a backslash (`\`):

    :::magpie
    val a = foo
    bar()
    // Sets a to foo then calls bar()

    val a = foo \
    bar()
    // Equivalent to:
    // var a = foo bar()
