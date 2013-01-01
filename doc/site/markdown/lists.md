^title Lists

Almost every language has arrays or lists and Magpie is no different. If you have a bunch of items that you want to lump together and identify programmatically, it's hard to beat them. A list is a compound object that holds a collection of *elements* identified by *index*. You can create a list by placing a sequence of comma-separated [expressions](expressions.html) inside square brackets:

    :::magpie
    [1, "banana", true]

Here, we've created a list of three elements. Notice that the elements don't have to be the same type.

## Accessing Elements

You can access an element from a list by calling the [subscript operator](multimethods.html#indexers) on it with the index of the element you want. Like most languages, indexes start at zero:

    :::magpie
    val hirsute = ["sideburns", "porkchops", "'stache"]
    hirsute[0] // "sideburns"
    hirsute[1] // "porkchops"

If you pass in an index that's greater than the number of items in the list, it [throws](error-handling.html) an `OutOfBoundsError`. If you pass in a *negative* index, it counts backwards from the end:

    :::magpie
    hirsute[-1] // "'stache"
    hirsute[-2] // "porkchops"

Of course, if you go *too* negative and shoot past the first item, that throws an error too. If you don't know how far that is, you can always find out by getting the number of items in the list using `count`:

    :::magpie
    hirsute count // 3
