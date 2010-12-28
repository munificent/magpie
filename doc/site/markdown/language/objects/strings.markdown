^title Strings
^index 3

### Strings

Strings are surrounded in double quotes:
    
    :::magpie
    "hi there"

A couple of escape characters are supported:

    :::magpie
    "\n" // Newline.
    "\"" // A double quote character.
    "\\" // A backslash.

Their class is `String`. Magpie strings are implemented internally using Java strings, so they are represented in UTF-16 format, although that shouldn't generally affect you. Most string operations in Magpie deal in logical characters, not bytes.

<p class="future">
Right now, getting a character out of a string returns another single-character string. Eventually, characters will be an atomic type in Magpie too.
</p>
