Every creative work betrays the author's beliefs about his fellow humans, and his secret desires about how the world should be. Some call these the "principles" by which a system is designed, but principles are uncomfortably close to dogma for my taste.

Instead, I'll call the guidelines below the Heart of Magpie. If you talk to programmers, you'll note that many have an emotional personal relationship with the languages they use day in and day out. Learning a new language is a bit like finding out if a partner is compatible with you, so consider this Magpie's answer to the language dating game:

### A programming language should be a medium of intent.

Imagine you're writing a function. You document what you intend the code to do
at a high level in some sort of pseudo-code and then you write the actual code.
The difference between those two blocks of text is a measure of the failure of
expressiveness in a language.

As much as possible, you should be able to define your own abstractions such that the code you write looks exactly like you would write on a whiteboard to explain to a colleague. The code should read like you think about your problem. Magpie should speak your language and not vice versa.

### Don't force users to predict the future.

In Java, it's standard practice to wrap all of your fields in getters and setters. That way, if in the future it turns out you need to do some calculation or validation, you won't have to touch every place that field is accessed.

Magpie tries to prevent you from needing to do speculative work like that. You
should be able to write code simple today that solves today's problems. Later,
when the problems change, the decisions you made today shouldn't come back to
haunt you.

This means following [uniform
access](http://en.wikipedia.org/wiki/Uniform_access_principle) so that built-in
default behavior today (such as accessing a field or assigning to a collection)
can be upgraded to a user-defined abstraction later without affecting every
callsite.

It also means allowing existing constructs to be extended and modified. You should be able add new methods or even fields to existing classes, even classes you didn't define. Classes should be able to implement interfaces that were created after the class was defined. Even the type system and syntax itself should be open to extension and modification.

### Give users power and trust them to use it well.

If smart users can't come up with things in Magpie that I didn't anticipate,
I've failed to design a sufficiently powerful language. To be long-lasting, a
language must be able to solve more than just the problems we know of today.
That means we have to be open to the language surprising us (but hopefully
mostly in pleasant ways).

"It's too powerful" should not be sufficient reason to deny users a feature. It's the scared, incompetent or cynical that are drawn to harmless things, and are those the people we want to surround ourselves with? We shouldn't minimize danger by removing powerful tools altogether, instead we should design them well such that they're easy and safe to use.

### Prefer defining things in the library over in the language.

One test of a languages power and expressiveness is seeing how many useful abstractions can be implemented just at the library level and don't require specific language support. The more you can do at the library level, the more power you've put in the hands of all users instead of just giving magical abilities to the language designers. Pushing things out to the library also gives a few very practical benefits:

* This simplifies the core language, which makes it easier to specify and
  implement.
* Libraries can evolve faster than a language can. This can increase the
  shelf-life of the language and give users a greater ability to explore
  different ways of expressing things.
* It makes it easier to port the language to other platforms. The more features
  that are written in the language itself, the less there is that needs to be
  written in the host language.
