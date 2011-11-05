^title Looping

Many languages have three distinct loop structures: `for`, `while`, and `foreach` or something similar for generators. Magpie only has a single loop construct, but it subsumes all three of those. A simple loop looks like this:

    :::magpie
    while something do
        body
    end

The `while something` part is the *clause*, and the expression after `do` (here a [block](blocks.html)) is the *body*. The clause is evaluated at the beginning of each loop iteration (including before the first). If it fails, the loop body is skipped and the loop ends. There are two kinds of clauses:

## `while` Clauses

The simplest is `while`. It evaluates a condition expression. If it evaluates to `false`, the loop ends, otherwise it continues. For example:

    :::magpie
    while 1 < 2 do print("forever...")

## `for` Clauses

The other loop clause is `for`. It looks like:

    :::magpie
    for item in collection do print(item)

The expression after `in` is evaluated *once* before the loop starts and is expected to return an *iterable* object, which it then passes to the `iterate` method. That should return an *iterator*.

An iterator generates a series of values. There are two methods it gets passed to. The `next()` method is called before each loop iteration (including the first) and advances the iterator to its next position. If the iterator is out of values, it returns `false` and the clause fails, otherwise it returns `true`. The `current` getter returns the current value that the iterator is sitting on.

Each iteration of the loop, Magpie will advance the iterator and bind the current value to a variable. The variable is scoped within the body of the loop. What this means is that a loop like:

    :::magpie
    for item in collection do print(item)

Is really just syntactic sugar for something like:

    :::magpie
    val __iter = collection iterate()
    while __iter next do
        val item = __iter current
        print(item)
    end

All of this means that Magpie *only* has a `foreach`-like for loop. It doesn't have a primitive C-style `for` loop. To get the classic "iterate through a range of numbers" behavior, the standard library provides methods on numbers that return iterators:

    :::magpie
    for i = 1 to(5) do print(i)
    // Prints "1", "2", "3", "4", "5".

    for i = 1 until(5) do print(1)
    // Prints "1", "2", "3", "4".

Here, `to` and `until` are just regular methods and not built-in keywords. They each return ranges that iterate through a series of numbers.

## Combining Clauses

The reason Magpie has only a single loop expression is because clauses can be combined. A single loop can have as many clauses as you want, of either kind, in any order. At the beginning of each iteration of a loop (including before the first one), *all* of the clauses are evaluated in the order they appear. If *any* clause fails, the loop body is skipped and the loop ends. For example:

    :::magpie
    while happy
    while knowIt do
        clap(hands)
    end

Once one of those `while` clauses returns false, the loop ends. Note that the clauses are iterated *in parallel*, unlike nested loops:

    :::magpie
    for i = 1 to(3)
    for j = 6 to(10) do
        print(i + ":" + j)
    end
    // Prints "1:6", "2:7", "3:8".

    for i = 1 to(4) do
        for j = 6 to(10) do
            print(i + ":" + j)
        end
    end
    // Prints "1:6", "1:7", "1:8", "1:9", "1:10", "2:7" ... "4:10".

The key difference is `do`. That's the keyword that indicates the end of the
clauses and the beginning of the body. No matter how many clauses you have, a
single `do` means a single loop.

Allowing multiple `for` clauses like this enables some handy idioms:

    :::magpie
    // Iterate through a collection with indexes
    for i = 0 countingUp
    for item = collection do
        // Here 'item' is the item and 'i' is its index.
    end

    // Iterate through the first 10 items in a collection.
    for i = 1 to(10)
    for item = collection do print(item)

    // Iterate through a collection until an item is found.
    var found = nothing
    while found == nothing
    for item = collection do
        if test(item) then found = item
    end

## Exiting Early

Inside the body of a loop, you can exit early using a `break` expression:

    :::magpie
    while true do
        something
        if badNews then break
        somethingElse
    end

## Return Value

Because loops are [expressions](expressions.html), they return a value.

<p class="future">
Right now, a loop will always return `nothing`, but that will likely change. It will be possible to define an `else` clause for a loop and then the expression will return the last result of evaluating the body, the expression passed to `break` or the expression after `else` if the body was never entered.
</p>
