^title How Do Operators Work?

In the current implementation, a binary operator is just a method call on the
left-hand argument. That's nice and simple, but doesn't actually work well in
practice:

*   You can do `"a" + 2` but not `2 + "a"`.
*   You can add a `|` operator to classes and other concrete type classes in
    order to create union types, but you can't add it to an interface, so 
    there's no way to do `Iterator | Nothing`. Ditto for `=>` and other type
    operators.
*   Every class that defines `==` has to manually mixin an implementation of
    `!=` based on it.

The more I think about it, the more lame it is. I think the core problem is that
we're baking in too many semantics. The language right now *defines* that 
operators are looked up in the method set of the left-hand argument and are
dispatched based on it. That's 1) too rigid and 2) honestly not the right
semantic for *any* operator.

There's a simple solution: just make operators functions instead of methods. So
an operator becomes a regular unbound function that take two arguments: the
left and right-hand side. For any given operator, the implementor of that
function can decide if any dispatch based on the arguments is appropriate. For
example, if we wanted to keep the current behavior for a certain operator, it
would be as easy as:

    :::magpie
    def ?!(left, right)
        left op?!(right)
    end

But if they *don't* want that behavior (which they actually don't), it can do
whatever it wants. For example:

    :::magpie
    def !=(left, right)
        (left == right) not
    end

Now we never have to mixin a `!=` operator. And:

    :::magpie
    def ++(left, right)
        concatenate(left string, right string)
    end

Now we have a symmetric string concatenation operator that works on all types.

The only downside is that operators sit in the global namespace (well not really
once actual namespaces are in). This means it gets trickier to define "local"
operators specific to a class. But my hunch is that those cases are rare and
when they do happen, it's easy to define a global one that just gives you that,
like the `?!` example above.

Because Magpie is mutable (at load time at least), you can always redefine an
operator function if the existing definition doesn't do what you want.

## Addendum

After implementing this, I've stumbled onto one other limitation of this 
approach: the type signature of the operator can now no longer vary based on the
argument type. With the previous approach, we could define `+` on Ints to return
and Int, and `+` on String to return a String. Now there's a single `+` function
with a single return type.

Generics may help here, but it will likely be an inevitable limitation of the
approach. In the specific example above, I fixed it by just making `+` no longer
used for string concatenation. Instead, `~` is used.




