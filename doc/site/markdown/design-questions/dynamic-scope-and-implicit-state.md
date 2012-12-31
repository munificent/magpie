## The Problem

When you define DSL-like APIs, you often want to have some implicit context
that the various methods can operate on. In other OOP languages that don't
require an explicit `this.`, this state is often stuffed into the containing
class, like TestSuite in jUnit. That lets your DSL have "bare" method calls
that read nicely.

Magpie doesn't have any way to pass along implicit state like this. Even the
"receiver" isn't implicit. This makes it harder to make DSLs that aren't
verbose. For example, the old test suite code looks like:

    :::magpie
    specify("An 'and' expression") with
        it should("return the first non-true argument") with
            (0 and false) shouldEqual(0)
            (1 and 0) shouldEqual(0)
            (1 and 2 and 0) shouldEqual(0)
        end
    end

That `it` parameter exists just to pass along the current test context.

## The Solution: Dynamic Scope

What would be nice is a way to push some context on the callstack where later
functions can then access. The traditional answer here is dynamic scope. That
doesn't play nicely with threads or callback-style async, but fortunately,
Magpie uses neither of those! Given fibers which make it easy to spin up and
suspend callstacks, the callstack is the perfect place to stuff this.

## Strawman

Here's a rough proposal. If you declare a variable whose name starts with `$`,
this creates a *dynamically scoped* variable. When you *access* a variable
with a name that starts with `$`, the VM will walk up the *callstack* to find
its value, and not the enclosing lexical scopes.

(If it fails to find the variable, it throws an `UndefinedVarError`. This is
a necessary corner case since it is no longer possible to statically determine
if this error can occur.)

For example:

    :::magpie
    def foo()
        val $i = "foo"
        baz()
    end

    def bar()
        val $i = "bar"
        baz()
    end

    def baz()
        print($i)
    end

    foo()
    bar()
    baz()

This program will print "foo", then "bar", and then thrown an
`UndefinedVarError`. Using this, we could make a test DSL like:

    :::magpie
    specify("An 'and' expression") do
        should("return the first non-true argument") do
            (0 and false) shouldEqual(0)
            (1 and 0) shouldEqual(0)
            (1 and 2 and 0) shouldEqual(0)
        end
    end

(Note no more `it` argument.)

It could be implemented like:

    :::magpie
    def specify(desc, body)
        var $spec = desc
        body call
    end

    def should(desc, body)
        var $desc = desc
        body call
    end

    def (actual) shouldEqual(expected)
        if actual != expected then
            print($spec + $desc + "expected " + expected + " but got " + actual)
        end
    end

## Caution!

Dynamic scoping is rightfully scary and frowned upon. Any time a `$` variable
is accessed, it can fail at runtime. Also, it's no longer possible to tell what
state a method can modify since it could be internally using dynamically-scoped
variables (though closures have similar issues along the lexical axis).

Like any powerful tool, it should be used in careful moderation.

One intent behind having a sigil character for dynamically-scoped names is that
it makes them stand out in code.
