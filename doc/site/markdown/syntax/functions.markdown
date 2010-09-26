^title Functions
^index 4

A language that claims to be functional needs to provide a simple syntax for  creating functions. Magpie uses the `fn` keyword for this. Here are some examples:

    :::magpie
    // single line:
    fn(a) print(a)
    fn(a, b) a + b
    
    // multiple line:
    fn(i)
        i = i * 2
        print(i)
    end

Anonymous functions can also have type annotations if you want. They follow the argument names:

    :::magpie
    fn(i Int) i * i
    fn(name String, age Int) print(name + " is " + age + " years old.")

Function expressions like this are just regular expressions. You can use them anywhere you like:

    :::magpie
    fn(outer)
        var a = fn(inner) fn(nested) print(inner + nested)
        takeFn(fn(a) a)
    end
    
    var ycombinator = fn(f) (fn(x) f(x(x))(fn(x) f(x(x))))

### Closures

As you would expect, functions are
[closures](http://en.wikipedia.org/wiki/Closure_%28computer_science%29): they
can access variables defined outside of their scope.

    var foo(i)
        // return a function that references a variable
        // defined outside of itself (i)
        fn() print(i)
    end
    
    var f = foo("hi")
    f() // prints "hi"

**TODO: Callable**