^title How Are Methods Defined?

Should methods always be declared inside some `class` or `extend` block, or just
be declared individually using `def`? There are two basic syntaxes for adding a
method to a class. The conventional OOP style is:

    :::magpie
    class SomeClass
        def someMethod(arg Int ->)
            // ...
        end
    end

To add a method to an existing class, just replace `class` with `extend`. The
other option is Go style, where methods are just declared freestanding, like:

    :::magpie
    def SomeClass someMethod(arg Int ->)
        // ...
    end

## Advantages for class style:

* Minimizes duplication when defining a lot of methods. Avoids repeating the
  class name for each method. With generic methods where the class name is an
  expression like `Dictionary[Key, Value]`, this can be a bigger deal.
* Familiar to most users.
* If we allow interface declarations to define methods in the main declaration,
  allowing classes to do the same would be more consistent.
  
## Advantages for Go style:
* Avoids an unneeded level of indentation.
* Emphasizes the openness of classes. Encourages people to add methods to
  arbitrary classes by making it lightweight to do so.
* Highlights the separation between state (the core class definition) and
  methods.

Answer: **There are advantages both ways.** If you're adding a lot of methods to one class, then being able to do that in one block saves a lot of redundant typing, especially with long class names or generic classes:

    :::magpie
    def AbstractDictionary[Key, Value] blah...
    def AbstractDictionary[Key, Value] blah...
    def AbstractDictionary[Key, Value] blah...
    def AbstractDictionary[Key, Value] blah...

On the other hand, if you're adding a bunch of methods to different classes (i.e. avoiding the visitor pattern), the blocks are tedious:

    :::magpie
    extend AddExpr
        evaluate(-> Int) left + right
    end
    extend IntExpr
        evaluate(-> Int) value
    end
    ...

The best solution may be to just support both.
