^title Records

Sometimes you need a little bundle of values where only a single one is expected. Maybe you want a division method that returns both the dividend *and* the remainder. To support this and much much more, as we'll see, Magpie has *records*. Records are anonymous, immutable composite objects.

Two little bits of syntax are used for them: fields and commas.

    :::magpie
    x: 1, y: 2

Here, we've created a record with two fields, `x` and `y`. A piece of text followed by a colon like `x:` defines a field. Any sequence of fields followed by expressions separated by commas defines a record. Note that the colon is part of the actual field token. This means you can use reserved names in fields, like `if:`, but also means you *can't* have a space before the `:` like `x :`.

## Accessing Fields

Once you have a record, you will likely need to pull it back apart. To do that, you'll use [pattern-matching](pattern-matching.html):

    :::magpie
    val point = x: 1, y: 2
    val x: a, y: b = point
    print(a + b) // 3

Record expressions and record [patterns](patterns.html) are duals of each other. Record expressions combine several values into a single object, and record patterns split those values back out.

## Positional Fields and Tuples

You can omit the field name, in which case the name will be inferred from its position. These lines are all equivalent:

    :::magpie
    0: "peanut butter", 1: "jelly
    "peanut butter", 1: "jelly
    "peanut butter", "jelly"

In other words, records in Magpie subsume both named records and [tuples](http://en.wikipedia.org/wiki/Tuple) in other languages. Records are first class values like any other [object](objects.html) which means they can be put into variables or passed to [methods](multimethods.html):

    :::magpie
    val point = x: 2, y: 3
    drawPoint(x: 2, y: 3)

In fact, *any* time you call a method that takes "multiple" arguments, you're actually just passing a record. Records are used pervasively through the language. Every time you see a comma in code, you're seeing a record or a record [pattern](patterns.html). (There's one exception, [lists](lists.html) use commas to separate elements too.)
