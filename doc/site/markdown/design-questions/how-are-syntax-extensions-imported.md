^title How Are Syntax Extensions Imported?

There's a weird little problem with extending Magpie's syntax. Consider this
example:

    :::magpie
    // a.mag
    definfix $$ 80 (left, right) ...

    // b.mag
    import("a.mag")
    print(1 $$ 2)

The intent here is that `b` imports `a` in order to define a syntax extension
(here a custom operator) and then uses it later in the file.

The problem is that in the current implementation, this won't work at all. `b.mag` will be completely parsed before that `import("a.mag")` is *evaluated*. So by the time we've defined `$$`, it's too late. Suck.

(The base library dodges this by being implicitly imported and evaluated completely before any user script is run.)

Some possible solutions:

## Declarative Imports

One option is to make imports not regular imperative Magpie code. Instead, they would be more like a C preprocessor directive. These directives would be scraped, parsed and processed (i.e. by importing referenced files) in a separate pass. After that completes and the imports are evaluated, the file is parsed again for the regular expression contents.

### Pros

*   Makes the dependency graph completely declarative and easier for tools to
    parse and scrape. Might make things like build tools easier to make.
*   Conceptually simple and familiar.

### Cons

*   Creates a separate little language. That goes directly against Magpie's
    "one language for everything" philosophy.
*   Not as flexible as using regular Magpie code to import. This would rule out
    things like conditionally importing a file or other programmable control
    over the import process.

## Script Header

This is similar to the above but more open-ended. We could allow the user to
define a section of the file as the "header". Everything in the header is parsed and evaluated. Then, after that, the rest of the file is parsed and evaluated.

This fixes the limitations of a special import language. The downside is that it
feels a bit arbitrary. Why allow just one header? What if you want three sections?

## Incremental Parsing

That takes us to the next solution: parsing and evaluating the file incrementally. The top level of a file is a series of expressions (and, importantly is *not* parsed as a single expression). Given that, it should be
fairly straightforward to parse and evaluate it at a piece at a time. Each time
a top-level expression is fully parsed, it is returned and evaluated before
continuing the parse.

This restricts incremental parsing to just the top-level which I think makes simpler to understand and less likely to be a limitation when it comes to implementing a bytecode compiler or otherwise optimizing. It avoids the need to add any special language features or syntax. In theory, at least, it should just do the Right Thing from the user's perspective.

