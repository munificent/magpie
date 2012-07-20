How Are Multimethods Compiled?

Given a set of method definitions with the same signature, how and when do we compile that to bytecode? Goals:

 1. We want to compile the method selection logic to regular bytecode. To tell
    if a given method matches, we don't want to have to walk the pattern tree
    in C++ like an interpreter.

 2. We want to select a method and bind its arguments at the same time. Testing
    a method and binding its arguments both involve destructuring, and we don't
    want to do that work twice.

 3. We want to detect pattern collisions at compile time.

 4. To make it possible to statically compile Magpie, it would be really nice if
    methods could be ordered without needing to first execute any Magpie code.

At a high level, the basic process for compiling a multimethod is.

 1. Get the set of methods. Since imports are static, this is pretty easy to do
    all at once during compile time (as long as we can ignore dynamic loading).

 2. Topologically sort them. The conditional logic for selecting a method needs
    to ensure that more specific methods are preferred over less specific ones.
    Specificity is a property of the multimethod itself (in that the actual
    runtime arguments passed to a method don't affect specificity).

 3. Compile them all to a single method whose body is (more or less) a match
    expression. Once we've ordered the methods, we can basically just compile
    it to a single method whose body is a pattern match with each methods as a
    case.

    This isn't *exactly* true because the jumping logic on match failure is a
    little different, and there's some special sauce needed to handle ambiguous
    methods, but it's roughly true. Given a set of methods and known
    specificity relationships between them, it should be doable to compile the
    whole thing to a chunk of pure bytecode.

The core nasty challenge is that step 2 here involves *executing* Magpie code.
Consider:

    def method(is Foo) ...
    def method(is Bar) ...

To order these methods, we need to know what `Foo` and `Bar` refer to: their
actual values. Those are expressions then that we need to evaluate. In this
case, we *could* say, that `is` patterns can only have class names on the right
and then we just treat class definitions specially so we can resolve them at
compile time.

But then consider:

    defclass Foo ...
    var Bar = if true then Foo
    def method(is Bar) ...

Ideally, this could work too, but that means allowing actual expressions for
type patterns. We could lose this flexibility and just say this use case isn't
supported, but then consider:

    defclass Color ...
    val red = Color new(...)
    val green = Color new(...)
    val blue = Color new(...)

    def method(== red) ...
    def method(== green) ...
    def method(== blue) ...

Here, we really do need to be able to support arbitrary expressions in value
patterns. We can't lose that without making value patterns basically useless.

Fortunately, we have an out here: value patterns don't have any relative
ordering, so we don't need to resolve their expressions to order methods that
use them. Win.

Except...

    val blue = Color new(...)
    val alsoBlue = blue

    def method(== blue) ...
    def method(== alsoBlue) ...

We really would still like to catch pattern collisions like this at compile
time.
