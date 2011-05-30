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

## Doc Comments

Magpie has a third kind of comment called *documentation comments* or simply *doc comments*. They start with three slashes and proceed to the end of the line.

    :::magpie
    def square(n is Int)
        /// Returns `n` squared.
        n * n
    end

Doc comments are used to document entire constructs: [classes](classes.html), [methods](multimethods.html), etc. Unlike other comments, doc comments are *not* ignored by the language. This means they are only allowed where they are expected: at the beginning of a method body or a class definition:

    :::magpie
    defclass Address
        /// A postal address.
        val street
        val city
        val state
    end

Doc comments are formatted using [Markdown](http://daringfireball.net/projects/markdown/) and are intended to be parsed to generate offline documentation files.

## Names

Identifiers come in two flavors in Magpie: regular names, and operators. A regular name is any sequence of letters, underscores (`_`), and periods (`.`). Digits may also be used after the first character. Case is sensitive.

    :::magpie
    hi
    ._.
    camelCase
    PascalCase
    abc123
    d.o.t
    ...
    ALL_CAPS

Operators are any sequence of punctuation characters from the following set:

    :::magpie
    ~ ! $ % ^ & * - = + | / ? < >

These are all valid operators:

    :::magpie
    +
    -
    *
    ?!
    <=>&^?!

The *only* difference between regular names and operators is the tokenization process&mdash; how the parser splits a series of characters into "words".

    :::magpie
    a+b

This will get parsed into three separate tokens: `a`, `+`, `b`. Once that tokenization is done, though, regular names and operators are both just identifiers. You can use either to name [variables](variables.html) and [methods](multimethods.html), [classes](classes.html), etc.

## Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // Two expressions:
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
doesn't make sense. Specifically, that means newlines following a comma (`,`), equals (`=`), backtick (<code>\`</code>), or infix operator (`+`, `-`, etc.) will be discarded:

    :::magpie
    var a = 1,
            2 // a will be the record (1, 2).

    var b = 1 +
            2 // b will be 3.

    var c = true and
            false // c will be false.

If you specifically want to ignore a newline where it otherwise *would* separate two expressions, you can end the line with a backslash (`\`):

    :::magpie
    var a = foo
    bar()
    // Sets a to foo then calls bar()

    var a = foo \
    bar()
    // Equivalent to:
    // var a = foo bar()
