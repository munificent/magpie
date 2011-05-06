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

## Whitespace

This flexibility has a side effect that might trip you up at first (but I
hope only at first): whitespace must be used to separate names, operators, and
literals. These examples will not be parsed like they would be in other
languages:

    a+b
    true==false
    count=3

Each line here is actually a single identifier. The solution is to make sure to
separate names with spaces, like `a + b`.

<p class="future">Note that the syntax highlighting is actually wrong in the examples here. Sorry. If you've got skill at hacking Pygments lexers, feel free to lend a hand.</p>

## Newlines

Like many scripting languages, newlines are significant in Magpie and are used to separate expressions. You can keep your semicolons safely tucked away.

    :::magpie
    // Two expressions:
    print("hi")
    print("bye")

To make things easier, Magpie will ignore a newline in any place where it
doesn't make sense. Specifically, that means newlines following a comma (`,`),
colon (`:`), operator (`+`, `-`, etc.), backtick (<code>\`</code>), arrow (`->`), or conjunction (`and` or `or`) will be discarded:

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

## Precedence

Magpie's syntax has fewer distinct levels of precedence than most languages. Many constructs start with a unique keyword (i.e. `var`, `class`, `if`, etc.) so don't need special precedence rules. For the core expression syntax, the precendence levels (from loosest to tightest) are:

1. Assigment (`=`)
2. Records (`,`)
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
