^title Patterns

If you've seen a few lines of Magpie code, you've likely seen patterns already. They are used everywhere in the language: [`match` expressions](pattern-matching.html) obviously use them, but so do [variable declarations](variables.html), [method parameters](multimethods.html), and `catch` clauses for [handling exceptions](error-handling.html).

Patterns are the foundation that control flow and naming are built on in Magpie. Given an [object](objects.html), a pattern does two things: First, it *tests* if the object *matches* that pattern. Then, if and only if it does, it may *bind new variables* to parts of the object. By performing those operations together, patterns can pull data out of an object but only when the object correctly has the data you're asking for.

A quick example in the context of a `match` expression should clarify:

    :::magpie
    match "s", 234, true
        case "s", n is Int, _ then print(n + 2)
    end

Here, the value being matched is the [record](records.html) `"s", 234, true`. The pattern is `"s", n is Int, _`, a record pattern containing a value pattern, type pattern, and wildcard pattern, respectively. This pattern *does* match that value, which means it will bind `n` and then evaluate the body in that scope. In this case, the body is just `print(n + 2)`.

## Kinds of Patterns

Syntactically, patterns are a bit like the twin of expressions. Like expressions, there are different atomic pattern syntaxes which can be composed to form larger patterns. There are six kinds of patterns:

### Literal Patterns

A [literal value](primitives.html) like `123` or `true` where a pattern is expected defines a *literal* pattern. As you would expect, a literal pattern only matches an identical value. The pattern `"hi"` matches the string value `"hi"` and nothing else.

### Equality Patterns

To check if a value is equal to the result of some [expression](expressions.html), you can use an *equality pattern*. It starts with `==`, followed by the value to be compared with.

    :::magpie
    == pi

The above pattern will match the value &pi; and fail to match otherwise.

### Type Patterns

Now we start to get to the interesting patterns. Often, you'll want to check to see if a value is of a certain [class](classes.html) (or a subclass) in order to tell if an operation is valid. To do that, you can use a *type pattern*. A type pattern starts with the keyword `is` followed by an expression:

    :::magpie
    is String

A type pattern matches if the value is of the given class or one of its subclasses.

### Variable Patterns

To bind values to names, you use *variable patterns*. A variable pattern always successfully matches, and when it does, it creates a new named variable whose value is the matched object. As you'd expect, a variable pattern is just an identifier.

    :::magpie
    name

This pattern matches any value and creates a new variable named `name` when it does. A variable pattern may also have another pattern following it. If it does, the variable pattern will only match if that pattern matches too. For example:

    :::magpie
    name is String

This is a variable pattern containing a type pattern. The entire pattern matches only if the value is a string. If it is, then it will bind the variable `name` to the value.

### Wildcard Patterns

If the name in a variable pattern is `_`, then it's a *wildcard* pattern. It works exactly like a variable pattern except that no variable will actually be created. This can be useful if you want to say "a value goes here" but you don't care what the value is. Like other variable patterns, it may also have an inner pattern.

### Record Patterns

*Record patterns* are the dual to [record *expressions*](records.html). A record pattern contains a series of fields. Each field may have a name, and must have a pattern. When it is tested, it looks for fields in the given value to match all of the pattern's fields. The entire record pattern matches if all of its field patterns match.

    :::magpie
    x: _, y: _

This will match any record with fields `x` and `y`. This is using simple wildcard patterns for the fields, but more complex patterns can be used:

    :::magpie
    x: 1, y: is String

This will match a record whose `x` field is `1` and whose `y` field contains a string. By using variable patterns for the fields, a record can be *destructured* into its component parts.

    :::magpie
    name: n, address: a

This will match a record with `name` and `address` fields. If it matches, it will create new variables `n` and `a` and bind them to the values of those fields.

Like record expressions, record patterns can omit the field names, in which case they'll be inferred by position:

    :::magpie
    x is Int, y is Int

This matches a record with two positional fields whose values are integers and binds the fields. In other words, matching that pattern against `3, 4` will bind `x` to `3` and `y` to `4`.
