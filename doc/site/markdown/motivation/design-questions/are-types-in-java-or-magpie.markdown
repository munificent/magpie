^title Are Types in Java or Magpie?

For a while, I've been working on making Magpie's type system Turing complete
such that type annotations are themselves regular Magpie expressions. This makes
Magpie very flexible: users can define classes that represent "type-like" things
and methods that create and manipulate types. For example, you could create a
type where only objects whose class starts with "B" are in the type.

This simplifies things in that there is only a single language for expressions
and type annotations and also meshes nicely with first-class classes: a type
annotation like `Int` really just looks up the global variable `Int` which
contains the object representing the class of integers.

The downside is that it makes the line between what's in Java and what's in
Magpie very blurry. Type-checking becomes a complicated dance that bounces
between the two languages. Lots of really weird edge cases are possible, such as
type expressions that aren't pure!

I beat my head on this for a while. I eventually got it mostly working, but it
still felt dirty. After a while I realized there were three main paths I could
take:

### Option 1: Follow the [COLA](http://en.wikipedia.org/wiki/COLA_%28software_architecture%29) concept of making a tiny core and having the language implemented in terms of itself.

So Java just has a core for defining objects and sending messages. Decisions
like prototypes versus classes are made entirely in Magpie. The entire type
system lives in Magpie.

#### Pros:

* Little switching between Magpie and Java.
* Very flexible: can do prototypes classes, etc. *simultaneously*.
* Academically interesting: not a lot of other languages like this.
* Small simple core.

#### Cons:

* Lot of work.
* Lot of stuff to write in Magpie, which could be a pain.
* Feels mushy.
* Unlikely to end up a usable language, more an experiment or novelty.

### Option 2. Keep going down the current path of a Turing-complete type system but with classes baked into Java.

#### Pros:

* Flexible extensible type system: can define new types in Magpie.
* May be able to use for dependent types or other out there concepts.
* Plays well with first-class classes.
* Turing-complete type system is interesting selling point.
* Have a single language for expressions and types, which is neat.
* Provides a natural path for generics: they are just expressions with type
  arguments that return new types.

#### Cons:

* Messy: lots of switching between Java/Magpie.
* Hard to tell at what level something should be implemented.
* Locks some type system choices (classes, inheritance) into code.
* Hard to merge semantics between what a magpie-level type object declares about
  an object and what the actual java backend lets you do with it (for example,
  you could define a "type" that says all object with methods that are
  palindromes are in the type, but there's no way to make an object actually
  have those methods). Classes are still special.

### Option 3. Pull back from flexibility and move the type system fully into Java.

It will still have first-class classes, but things like or types `Int | Bool` and interfaces would be implemented in Java. No more type expressions. Instead, there'd be a distinct syntax for type annotations.

#### Pros:

* Simple, conventional and practical.
* Don't have to switch back and forth between Magpie/Java when type-checking.
* Makes some type declaration syntax (function types) easier to parse.
* Most well-worn path to a usable language.

#### Cons:

* Not as novel or flexible.
* Type system is baked into Java: users can't define their own type-like things.
* Need to figure out how generics work with this since they need to essentially
  create new types at check time.

Answer: **Option 2.**

Option 3 is ruled out because all types do in practices need to be first-class
anyway (see [this doc](are-all-types-first-class.html) for why). That means the
types will need to exist in Magpie, so also implementing them in Java would just
be redundant and confusing.

So that leaves options 1 and 2. I'm open to option 1, but I fear that's going
too far down the flexibility rabbit hole. It may make it hard/slow/cumbersome to
statically determine which objects meet which types since there's no core
built-in types to build on. I may investigate it further, but for now, I'll
stick with the middle ground. My suspicion is that, over time, it will gradually drift towards option 1.