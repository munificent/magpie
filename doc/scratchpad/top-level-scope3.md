Any expression can appear in any order at the top level. Top-level expressions, including `def`, `var`, and `defclass` are executed top-to-bottom. Unlike in nested scopes, any name defined at the top level can be referred to anywhere in the program, even before that definition appears. However, an expression containing a name must not be *executed* before that name is defined.

Expressions appearing is variable initializers are executed immediately at the point of the definition, as are superclass specifiers. Method bodies, method patterns, field initializers, and field patterns are *not* executed immediately.

These are errors:

    var first = second
    var second = "value"

    defclass First is Second
    end
    defclass Second
    end

This is OK:

    def method() later
    var later = "ok"
    print(method())

But note that this is not:

    def method() later
    print(method()) // later has not been defined yet
    var later = "ok"

To avoid errors, all referred-to names but have defined values (i.e. the expression that defines it must have been executed) before expressions containing them are executed.

 *  A method body is executed when that multimethod is called and that specific
    method is selected and executed.
 *  A method pattern is executed once when that multimethod is called after
    that method's `def` expression has been executed. Field patterns are
    essentially method patterns too.
 *  A field initializer is executed when an instance of the containing class is
    constructed. (And when a value for that field is not provided?)

The general model here is that instead of trying to statically detect undefined variable access errors, we will defer much of that to runtime. This doesn't catch as many errors as we'd like, but it makes top-level forms simpler to reason about.