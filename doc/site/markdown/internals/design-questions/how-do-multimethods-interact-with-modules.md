^title How Do Multimethods Interact with Modules?

Consider:

    module a
        def double(i Int) i * 2
    end

    module b
        def double(s String) s ~ s
    end

    module c
        import a
        import b

        double(1)
        double("s")
    end

Assuming that `import` means, "get all of the top-level identifiers in the given module and copy them into this one", then that code has a problem. Modules `a` and `b` each define multimethods bound to top-level variables `double`. When we hit `import b`, it will either have a name collision when it imports `double` again, or it will overwrite. In neither case will the code do the right thing.

This is a bummer since namespaced methods was the motivating factor for multimethods (i.e. not tied to class generic functions) to begin with. What's the solution?

## Option 1: Module-level Multimethods

One option would be to not make multimethods first-class. Right now, they're just objects bound to a variable. Named dispatch looks up the variable, finds a multimethod in it, and goes from there. This means that they're pretty opaque when it comes to things like `import`.

On the other hand, it means you can pass them around as objects. Except you actually can't because there's no syntax to just get one. If you have a multimethod stored in `foo`, then just doing `foo` won't get it. Instead, it will try to invoke it as a getter on the current (implicit) receiver: i.e. it calls `this foo`.

So, since they aren't very first-class like anyway, one option would be to make them explicitly not first class and more built into the language as a module-level construct. When you declare a method, it adds it to the current module. When you import it, it just merges it with any methods of the same name in the current module.

### What About Functions?

This makes them very different from functions, which are normal, lexically scoped first-class objects. It's clear that functions are still needed for things like `with` and higher-order functions like `map` and `filter`. Is it redundant or confusing to have both?

There's already some difference between a method and a function even in single-dispatch Magpie since `this` has different semantics between the two. This pushes them farther apart, but that may not be a bad thing.

### What About Local Multimethods?

One consequence of this is that there's no way to make a "local" multimethod. Regardless of where the `def` appears, they live at the module level. That covers 95% of the use cases, but is a limitation. At the very least, it will make the spec a little trickier since it wants to scope things as locally as possible.

This also feels like it goes against the grain of the language since pretty much everything else is lexically scoped: functions, classes, variables.

## Option 2: Merge on Import

Perhaps a more straightforward solution is to simply merge multimethods on import. If you import a named multimethod and there is already a multimethod with that name in the current module, just merge them.

