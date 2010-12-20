Q: How Do We Handle Abstract Methods?

### Use Cases

1.  Extension methods in C#. We should be able to define a mixin for iterable
collections. So, if a class implements `iterate()`, you can mixin another class
that will give it `first`, `each()`, etc. That mixin class needs to implement
those in terms of `iterate()`, which it does *not* define. In the context of the
mixin, `iterate()` is an abstract member.

2.  "Base class"-style delegate fields. For example, Amaranth has a class like this:
    
        :::csharp
        abstract class ContentBase
        {
            public abstract string Name { get; }

            private Content content

            ContentBase(Content content)
            {
                this.content = content;
            }
        }
    
    In Magpie, this would be a delegate field since it has state, but we need
    to be able to support that abstract "Name" property.

### Requirements

1.  A user should be able to declare (as opposed to define) a member in a class.
    A declared member has a type but no implementation. When type-checking a
    class, it will be as if that member is there. This way, we can type-check a
    mixin or delegate class in the context of having the functionality it needs
    its host to provide.

2.  We should be able to statically ensure that all declared members are given
    an implementation before they can be accessed. A user shouldn't have to
    worry about getting an error at runtime that they tried to call a method
    that wasn't given a concrete implementation. If there's a member they need
    to implement, it should tell them this at check time.

3.  You should be able to "forward" abstract members. For example, class A may 
    define an abstract member `foo`. Class B mixes in A but is also intended to
    be used as a mixin. It should be able to declare an abstract member `foo`
    that passes the buck onto the class that mixins *it* in.

5.  As always, we should accomplish this with a minimum of ceremony and
    complexity.

### Abstract Mixins

Mixins are the easy one. Since they are already stateless (non-constructible), all we really need to do is:

1.  Allow the user to declare members on a class.
2.  When we're checking a mixin, make sure that the parent class defines members
    that are compatible with all of the declared members on the mixin.
3.  If a class has abstract members, don't let it be constructed.

And that should be good. Rule 2 lets us implement abstract members. Rule 3 and the fact that mixins side-step construction completely make sure that you will only be able to refer to an abstract member from a context where a concrete implementation has been provided.

### Abstract Delegates

Now we come to the challenge. Lets say we wanted to implement the ContentBase use case in Magpie. The abstract delegate class would be:

    :::magpie
    class ContentBase
        def shared new(content Content -> ContentBase)
            construct(content: content)
        end
        
        get name String

        var content Content
    end

A class using it would be something like:

    :::magpie
    class WidgetContent
        def shared new(content ContentBase)
            construct(content: content)
        end
        
        delegate var content ContentBase
    end

So we'd construct one like:

    :::magpie
    var content = ContentBase new(Content new(...))
    var widget = WidgetContent new(content)

Magpie's construction style is from the leaves in: we create all of the fields
for a class and then instantiate the class using them. The problem here is the
first line. At that point, we're creating an instance of an *abstract* class. That violates our second requirement. There's nothing here preventing you from
doing:

    :::magpie
    var content = ContentBase new(Content new(...))
    content name // bad! calling abstract member!

Even if we don't forget to give it a parent object, there's an equivalent problem:

    :::magpie
    var content = ContentBase new(Content new(...))
    var widget = WidgetContent new(content)
    // fine so far...
    widget name // delegates to content through widget, still ok...
    widget content name // bad! not going through widget, so we won't be able
                        // to look up the name member on it

Ideas to resolve this:

#### Don't allow abstract delegates

The simplest and harshest solution. Just don't allow abstract methods in delegates. In practice, I don't think this will work well. I've got lots of examples in Amaranth and other code of classes with both state and abstract members.

#### Define two types for an abstract class

Given an abstract class like `ContentBase`, there will be *two* types: `ContentBase` and `AbstractContentBase`. The first is the "normal" type and can be used like you'd expect. The only objects that will have this type are places where the abstract members have been correctly implemented by a delegating parent object. So, in the above example, `WidgetContent` is a subtype of `ContentBase` because it has a delegate field of that class.

The `AbstractContentBase` type is then for variables of the abstract class that are not correctly accessed through a delegating parent that implements its abstract members. When you construct an instance of `ContentBase` the variable you get back is of type `AbstractContentBase`. If you access a delegate field on some object (like doing `widget content`) that's the type of variable you'll get back since you're stripping off the delegating parent.

The `Abstract__` type has no members on it. This ensures that you'll get a check error if you try to use an instance of an abstract type outside of its delegating content. It's basically a black hole. You can pass it around, but you can't do anything with it.

When a class has a delegate field (with abstract members), its `construct` method will take the *abstract* type for that field, not the normal one. So, in the above example, the type signature for `WidgetContent construct` is `content: AbstractContentBase`.

In other words, an instance of an abstract class has a special not-very-useful `AbstractFoo` type. But a class that has a delegate field of that class is correctly a subtype of the full-featured `Foo` type.

#### Make the type useless

This is a refinement of the previous idea. Instead of defining two types, just
have one. But that type will be the equivalent of the `Abstract__` up there: it
will have no members. It's assignable to itself, but aside from that, there 
isn't anything useful you can do with it. That just gives you enough to get it
into a delegate field for class that's using it, which is all you need.

This implies that abstract classes do not define any usable type. They provide
stateful behavior that can be mixed into another class, but don't define a type
that describes all classes that do that. For example:

    :::magpie
    class Named
        def sayName(->) print("Hi, I'm " + name + (if excited? then "!" else "."))
        get excited? Bool // abstract
        var name String
    end

    class Dave
        get excited? Bool = true
        delegate var name Named
    end

Given these, you can create a new `Dave` like this:

    :::magpie
    var dave = Dave new(name: Named new(name: "Dave"))

What you can't do is define functions that act on the `Named` interface alone:

    :::magpie
    def sayTwice(named Named ->)
        named sayName()
        named sayName()
    end

The problem is that the `Named` type has no members, not even `sayName`. That
ensures that you don't try to use a standalone instance of an abstract class,
but also prevents the above.

This seems like enough of a limitation that it probably isn't worth pursuing.

#### Implicitly construct delegate fields

Consider the above example:

    :::magpie
    var dave = Dave new(name: Named new(name: "Dave"))

The key bit here, and the cause of our problems, is that we're passing in an
instance of `Named` to that constructor. But we can't safely create one of those
outside of the context of a parent class. So maybe the solution is to not do
that. Instead, the containing class's `construct` function will have the magic
required to promote a *record* for the delegate field to the real deal. The
above would become:

    :::magpie
    var dave = Dave new(name: (name: "Dave"))

And then, internally, it will take that `name: "Dave"` record and promote it to
the delegate field's type.

This neatly solves the problem of dangling delegates. Abstract classes simply
won't have constructors and cannot be created on their own. They can still be
used as types. We'll just have to modify the class subtyping rules to allow a
class A to be assignable to class B if A has a delegate field of type B.

#### Don't worry about it

Remember, Magpie is optionally typed. It isn't perfectly sound. Maybe the
simplest solution is to not worry about it. You can instantiate an abstract
class just fine. If you try to use it, it'll do weird (but defined!) things when
it tries to call abstract members. Don't sweat it.

