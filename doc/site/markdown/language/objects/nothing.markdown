^title Nothing
^index 4

Magpie has a special value `nothing`, which is the only value of the class
`Nothing`. (Note the difference in case.) It functions a bit like `void` in some
languages: it indicates the absence of a value. An `if` expression with no
`else` block whose condition is `false` evaluates to `nothing`. Likewise, a
function like `print` that doesn't return anything actually returns `nothing`.

It's also similar to `null` in some ways, but it doesn't have [the
problems](http://journal.stuffwithstuff.com/2010/08/23/void-null-maybe-and-nothing/)
that `null` has in most other languages. It's rare that you'll actually need to
write `nothing` in code since it can usually be inferred from context but it's there if you need it.