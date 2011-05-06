^title Records

**TODO: This is out of date now that tuples and records are unified.**

A record is similar to a [tuple](tuples.html) but where the fields are
identified by name instead of number. Another way to look at them is as
anonymous structures. The syntax is:

    :::magpie
    x: 1, y: 2
    // Creates a record with fields "x" and "y"
    // Whose values are 1 and 2, respectively.

While records look like dictionaries or maps in some languages, they have one
important difference: they are strongly-typed. The set of fields and their types
is part of the *type* of a record. A record `x: 1, y: 1` has a different static
type than `x: 1, y: 1, z: 1` or even `x: 1, y: "a string"`. (If you're familiar
with any of the ML languages, this is familiar territory.)

## Accessing Fields

Fields in a record can be accessed like any other field on an object, by name:

    :::magpie
    var point = x: 1, y: 2
    print(point x) // Prints "1".
    print(point y) // Prints "2".

Records are immutable, so fields cannot be assigned to.
