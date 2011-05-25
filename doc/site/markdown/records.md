^title Records

Sometimes you need a little bundle of values where only a single one is expected. Maybe you want a division method that returns both the dividend *and* the remainder. To support this and much much more, as we'll see, Magpie has *records*. Records are anonymous composite objects.

Two little bits of syntax are used for them: fields and commas.

    :::magpie
    x: 1, y: 2

Here, we've created a record with two fields, `x` and `y`. A piece of text followed by a colon like `x:` defines a field. The colon is part of the actual token, which means you can use reserved names in fields, like `if:`, but means you *can't* have a space before the `:`. Think of them is being a separate kind of literal like strings.

Any sequence of fields followed by expressions separated by commas defines a record. They can also be nested arbitrarily deeply:

    :::magpie
    address: (street: "123 Main St.", city: "Gary", state: "IA"),
    phone: "867-5309"

## Accessing Fields

Once you have a record, you will likely need to pull it back apart. To do that, you'll use [pattern-matching](pattern-matching.html):

    :::magpie
    val numbers = 1, 2, 3
    val x, y, z = point
    print(x + y + z) // 6

Record expressions and record patterns are duals of each other. Record expressions combine several values into a single object, and record patterns split those values back out.

## Positional Fields and Tuples

You can omit the field name, in which case the name will be inferred from its position. These lines are all equivalent:

    :::magpie
    0: "peanut butter", 1: "jelly
    "peanut butter", 1: "jelly
    "peanut butter", "jelly"

In other words, records in Magpie subsume both named records and tuples in other languages. Records are first class values like any other [object](objects.html) which means they can be put into variables or passed to [methods](multimethods.html):

    :::magpie
    val point = x: 2, y: 3
    drawPoint(x: 2, y: 3)

In fact, *any* time you call a method that takes "multiple" arguments, you're actually just passing a record. Records are used pervasively through the language. Every time you see a comma in code, you're seeing a record (or a record [pattern](patterns.html)).

Records, like other primitive types in Magpie, are *immutable*. Once you've created one, you can't assign to any of its fields.
