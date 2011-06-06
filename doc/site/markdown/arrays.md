^title Arrays

Almost every language has arrays and Magpie is no different. If you have a bunch of items that you want to lump together and identify programmatically, it's hard to beat them. An array is a compound object that holds a collection of *elements* identified by *index*. You can create an array by placing a sequence of comma-separated [expression](expressions.html) inside a pair of square brackets:

    :::magpie
    [1, "banana", true]

Here, we've created an array of three elements. Notice that the elements don't have to be the same type.

## Accessing Elements

You can access an element from an array by calling the [indexer method](multimethods.html#indexers) on it with the index of the element you want. Like most languages, indexes start at zero:

    :::magpie
    val hirsute = ["sideburns", "porkchops", "'stache"]
    hirsute[0] // "sideburns"
    hirsute[1] // "porkchops"

If you pass in an index that's greater than the number of items in the array, it [throws](error-handling.html) an `OutOfBoundsError`. If you pass in a *negative* index, it counts backwards from the end:

    :::magpie
    hirsute[-1] // "'stache"
    hirsute[-2] // "porkchops"

Of course, if you go *too* negative and shoot past the first item, that throws an error too. If you don't know how far that is, you can always find out by getting the number of items in the array using `count`:

    :::magpie
    hirsute count // 3

## Immutability

One point bears repeating because it's a bit unusual: arrays are *immutable* in Magpie. Once you've created one, you can't add to it, swap out elements, or clear it:

    :::magpie
    hirsute[1] = "goatee" // ERROR!

If you want a *mutable* indexed collection, you want a *list*. You can create a list from an array like so:

    :::magpie
    val beards = hirsute toList
    beards[1] = "goatee" // OK!
