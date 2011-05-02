^title Patterns

If you've seen a few lines of Magpie code, you've likely seen patterns already. They are used everywhere in the language: `match` expressions use them, but so do variable declarations, method parameters, and `catch` clauses for handling exceptions.

Given an object, a pattern does two things: First, it tests if the object *matches* that pattern. Then, if and only if it does, it may bind new variables to parts of the object. By performing those operations together, it's able to safely pull data out of an object but only when the object has the data you're asking for.

## Kinds of Patterns

There are a few different basic patterns built in:

### Wildcard Patterns

The simplest pattern is the wildcard. It looks like this:

    :::magpie
    _

A single underscore defines a wildcard pattern. Wildcards always successfully match, but don't bind any variables.

### Literal Patterns

A literal value like `123` or `true` where a pattern is expected defines a *literal* pattern. As you would expect, a literal pattern only matches an identical value. The pattern `"hi"` matches the string value `"hi"` and nothing else.

### Equality Patterns

To check if a value is equal to the result of some expression, you can use an *equality pattern*. It starts with `==`, followed by the value to be compared with.

    :::magpie
    == math.Pi

The above pattern will match the value &pi; and fail to match otherwise.

### Type Patterns

Now we start to get to the interesting patterns. Often, you'll want to check to see if a value is of a certain class (or a subclass) in order to tell if an operation is valid. To do that, you can use a *type pattern*. A type pattern starts with the keyword `is` followed by a name:

    :::magpie
    is String

A type pattern matches if the value is of the given class or one of its subclasses.

### Variable Patterns

To pull data out of an object, you use *variable patterns*. A variable pattern always successfully matches, and when it does, it creates a new variable with its name whose value is the object being matched. As you'd expect, a variable pattern is just an identifier. This pattern matches any value and creates a new variable named `name` when it does:

    :::magpie
    name

A variable pattern may also have another pattern following it. If it does, the variable pattern will only match if that pattern matches too. For example:

    :::magpie
    name is String

This pattern will match if the value is a string. If it is, then it will bind the variable `name` to the value.

### Record Patterns

As you can imagine, these are the dual to [record expressions](expressions/records.html). A record pattern contains a series of fields. Each field may have a name, and must have a pattern. When it is tested, it looks for fields in the given value to match all of the pattern's fields. The entire record pattern matches if all of its field patterns match.

    :::magpie
    x: _, y: _

This will match any record with fields `x` and `y`. This is using simple wildcard patterns for the fields, but more complex patterns can be used:

    :::magpie
    x: 1, y: is String

This will match a record whose `x` field is `1` and whose `y` field contains a string. By using variable patterns for the fields, a record can be *destructured* into its component parts.

    :::magpie
    name: n, address: a

This will match a record with `name` and `address` fields. If it matches, it will create new variables `n` and `a` and bind them to the values of those fields.

Like record expressions, record patterns can omit the field names in which case they'll be inferred by position:

    :::magpie
    x is Int, y is Int

This matches a record with two positional fields that are integers and bind the fields. In other words, matching that pattern against `3, 4` will bind `x` to `3` and `y` is `4`.

## Patterns in Expressions

We've seen patterns in variable declarations, but they show up in other places too. The most obvious is `match` expressions:

    :::magpie
    match couple
        case him: "Ralph", her: "Alice" then "Pow! Right in the kisser!"
        case him: "Ricky", her: "Lucy"  then "Lucy! I'm home!"
    end

