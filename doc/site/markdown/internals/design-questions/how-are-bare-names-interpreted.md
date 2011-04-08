^title How Are Bare Names Interpreted?

A bare name in code like `i` can be interpreted as either a variable in some surrounding scope, or as a getter on `this`. It could correctly be both of those at the same time, so it's important to know which takes priority and when.

I believe the expected behavior is:

    :::magpie
    var i = "global"
    print(i) // global
    defclass C
        var i = "getter"
    end

    do
        var i = "outer"
        def (this C) method() ->
            print(i) // getter
            var i = "local"
            print(i) // local
            do
                var i = "shadow"
                print(i) // shadow
            end
            print(i) // local
        end
    end

So local variables within the method take priority over getters, which take priority over variables declared in the method's closure. Lookup then is:

1.  Look in the current local scope.
2.  Walk up local scopes until we hit the top scope of the function body.
3.  Look on `this`.
4.  Look in the scope chain where the method is declared (its closure).
5.  If not found, throw an `UnknownNameError`.

That's fine for things with only a single level of nesting. Now consider:

    :::magpie
    var i = "global"
    defclass C
        var i = "getter"
    end

    def (this C) method1()
        var i1 = "1"
        def (this C) method2()
            var i2 = "2"
            def (this C) method3()
                var i3 = "3"
                print(i1) // "1"
                print(i2) // "2"
                print(i3) // "3"
                print(notfound)
            end
        end
    end

When we look up `notfound` here, it needs to:

1.  Look in the locals of `method3`.
2.  Look for a getter on `this` in `method3`.
3.  Look in the locals of `method2`.
4.  Look for a getter on `this` in `method2`.
5.  Look in the locals of `method1`.
6.  Look for a getter on `this` in `method1`.
7.  Look in the global scope.

So we need either a way to arbitrarily interleave lookup of variables and getters in the scope chain, or we need to restrict the scope in which methods can be defined (which is kind of lame).

This gets more confusing because getters themselves are defined in the scope chain. It's just that we may need to "find" them earlier when walking the scope chain. For example:

    :::magpie
    // 1
    defclass C
        var i = "getter"
    end
    // getter is defined here in scope chain

    do // 2
        var i = "inner"

        def (this C) method() // 3
            print(i)
        end
    end

Here we have three nested scopes (labelled `// 1`, `// 2`, `// 3`). The getter for `i` is defined in `1`, the outermost scope. There is a variable `i` declared in `2`. Inside `3`, we print `i`. The tricky bit is that it needs to find the getter first, so we need to give priority to the `i` in `1` over the nearer one in `2`. Confusing.
