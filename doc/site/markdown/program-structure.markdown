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

## Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // Two expressions:
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
doesn't make sense. Specifically, that means newlines following a comma (`,`),
colon (`:`), operator (`+`, `-`, etc.), or open brace (`(`, `[`, `{`) will be
discarded:

    :::magpie
    var a = 1,
            2 // a will be the tuple (1, 2).

    var b = 1 +
            2 // b will be 3.

    print(
        "hi") // Prints "hi".

If you specifically want to ignore a newline where it otherwise *would* separate two expressions, you can end the line with a backslash (`\`):

    :::magpie
    var a = foo
    bar()
    // Sets a to foo then calls bar()

    var a = foo \
    bar()
    // Equivalent to:
    // var a = foo bar()

## Precedence

Magpie's syntax has fewer distinct levels of precedence than most languages. Many constructs start with a unique keyword (i.e. `var`, `class`, `if`, etc.) so don't need special precedence rules. For the core expression syntax, the precendence levels (from loosest to tightest) are:

1. Assigment (`=`)
2. Tuples and records (`,`)
3. Conjunctions (`and`, `or`)
4. Operators (`+`, `-`, `?$!`, etc.)
5. Messages (`print(foo)`, `list count`, etc.)

Some examples will clarify. The comment after each line is how the parser interprets that expression:

    :::magpie
    a = b, c            // a = (b, c)
    a and b, c or d     // (a and b), (c or d)
    a or b + c          // a or (b + c)
    a b + c             // (a b) + c
    a = b c, d - e or f // a = ((b c), ((d - e) or f))

Parentheses can be used for grouping to override this as you'd expect:

    :::magpie
    a or b + c    // a or (b + c)
    (a or b) + c  // (a or b) + c

Tuples and records exist at the same precedence level, so cannot be mixed:

    1, 2, 3           // a tuple
    a: 1, b: 2, c:, 3 // a record
    a: 1, 2           // bad: need a field name before "2"
    1, a: 2           // bad: not expecting field name in tuple

To mix them together, use parentheses:

    a: (1, 2) // ok: a record with one field that's a tuple
    1, (a: 2) // ok: a tuple whose second field is a record
