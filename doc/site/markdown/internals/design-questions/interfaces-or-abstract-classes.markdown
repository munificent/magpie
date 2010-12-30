^title Interfaces or Abstract Classes?

Instead of having interfaces, C++ just has classes but you can define a class where all of its methods are abstract. With multiple inheritance, a C++ class can effectively implement multiple interfaces. Should Magpie have abstract methods? Does it need interfaces at all?

## Advantages for interfaces:

* Popular and familiar.
* Multiple inheritance is complex. Have to figure out how methods are looked up,
  how collisions are dealt with, how constructors are invoked, etc.
* Explicit. Interfaces cannot have implementation.

## Advantages for abstract classes:

* Much more flexible. An abstract class can have some concrete methods.
* Just one kind of construct: the class. Of course, multiple inheritance adds
  complexity to offset that simplicity.
* Multiple inheritance is powerful and useful in its own right.
* Magpie's dynamic object-is-a-dictionary underlying system should simplify some
  of the problems with multiple inheritance that C++ has.

Answer: **It Should Have Interfaces.**

Unlike classes, interfaces in Magpie allow implicit compatibility: a concrete type that has matching methods is compatible with the interface automatically. That allows a measure of duck typing, but it's *not* something I think all classes should allow. I don't think arbitrary concrete classes should be implicitly compatible based just on method compatibility.

This doesn't necessarily rule out abstract classes or methods too: they serve a different purpose.