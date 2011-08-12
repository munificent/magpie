^title Errors

Even the best-intentioned programs and users occasionally go awry and a language must give you tools to handle them. Magpie has a few tricks up its sleeve for working with errors and exceptional conditions.

## Returning Errors

For errors that are common or important enough that you want to ensure programmers handle them, you can simply return an error object. Magpie defines an `Error` class that is the base class for objects representing errors. Let's say we're writing a method that converts a "yes" or "no" string to a boolean. We can implement that like this:

    :::magpie
    def parseYesNo(value is String)
        match value
            case "yes" then true
            case "no"  then false
        end
    end

What happens if the passed in value is neither "yes" or "no"? A simple solution is to return an error.

    :::magpie
    def parseYesNo(value is String)
        match value
            case "yes" then true
            case "no"  then false
            else ParseError new("String must be 'yes' or 'no'.")
        end
    end

This pushes the problem onto the caller. They can no longer assume `parseYesNo` will always return a `Bool`, since it may now also return a `ParseError`. Pattern-matching gives them a straightforward way to distinguish those cases.

    :::magpie
    var response = getTextFromUser()
    match parseYesNo(response)
        case b is Bool       then "Got good answer"
        case e is ParseError then "Bad input"
    end

## Throwing Errors

Some errors occur very rarely, such as a stack overflow or out of memory. Other errors indicate bugs in the code that should be fixed instead of handling the error at runtime. For those cases, it's a chore to make the user check and manually handle an error return that they never expect to see.

For those cases, Magpie also supports *throwing* errors. A `throw` expression will cause the currently executing code to immediately stop and being unwinding the callstack. A `throw` includes an error object that describes the problem. By convention, these should be subclasses of `Error`.

Here is a "safe" division function that does not allow dividing by zero. Since attempting to divide by zero indicates a programmer error, it throws instead of returning the error.

    :::magpie
    def safeDivide(numerator is Int, denominator is Int)
        if denominator == 0 then throw DivideByZeroError new()
        numerator / denominator
    end

## Catching Errors

Unlike a returned error, a thrown error will not be given back to the calling code. Instead, Magpie will continue to unwind the callstack, causing each successive method to immediately return until the error is *caught*.

Errors are caught using a *catch clause*, which is `catch` followed by a [pattern](patterns.html), followed by `then`, and finally the expression or block to execute when an error is caught.

    :::magpie
    def canDivide(numerator is Int, denominator is Int)
        safeDivide(numerator, denominator)
        true // If we got here, no error was thrown.
    catch err is DivideByZeroError then
        false // If we got here, an error occurred.
    end

You can see here that unlike the exception-handling syntax in most languages, Magpie does not have an explicit `try` syntax. Instead, *any* [block](blocks.html) is implicitly a "try" block and may have catch clauses. In `canDivide`, the block is the method body itself, but other blocks may have catch clauses. For example:

    :::magpie
    if someCondition then
        doSomethingUnsafe()
    catch err is Error then
        // Failed.
    else
        doSomethingElse()
    catch err is Error then
        // Also failed.
    end

(There is one exception to this rule. A block that defines a `catch` clause's body may not have its own `catch` clauses.)

A single block may have more than one catch clause. When an error is thrown from the block, each catch clause's pattern is matched against the error in the order that they appear. The first catch clause whose pattern matches catches the error. The body of the catch clause is evaluated and that becomes the value returned by the block.

If no catch clause matches the error, the error continues to propogate.

## Errors and "Exceptions"

Magpie's error-handling system is very similar to exceptions in most languages. It uses the term "error" for them because its valid to use errors outside of `throw` and `catch`: you can return error objects and pass them around, which is considered poor form in languages that refer to them as exceptions.

The caller and callee may also disagree on whether or not an error is important enough to be returned or should be thrown. In those cases, a caller may catch a thrown error and return it, or throw a returned one. Using the same `Error`-derived classes for both affords that flexibility.

    :::magpie
    def returnError()
        doSomethingThatThrows()
    catch err is Error then
        err // Return it.
    end

    def throwError()
        match doSomethingThatReturnsError()
            case err is Error then throw err
            case success then success
        end
    end

In other words, an `Error` object tells you *what* the error is, but now how it gets passed from the code that generates the error to the code that handles it.
