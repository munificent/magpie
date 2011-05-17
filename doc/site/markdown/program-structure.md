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

**TODO**

## Names

Magpie is more open-ended when it comes to names than most other languages. Regular identifiers, used for things like [variables](variables.html) and [methods](multimethods.html) can consist of letters, numbers, underscores, and even many punctuation characters. These are all valid names:

    :::magpie
    hi
    +
    using-hyphens
    numb3r5_r_0k_2
    even_!$%_this_^&*_is_<>=_valid
    $%^*
    a.b.c
    ...

## Whitespace

This flexibility has a side effect that might trip you up at first (but I
hope only at first): whitespace must be used to separate names, operators, and
literals. These examples will not be parsed like they would be in other
languages:

    :::magpie
    a+b
    true==false
    count=3

Each line here is actually a single identifier. The solution is to make sure to
separate names with spaces, like `a + b`.

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
            2 // a will be the tuple (1, 2).

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
