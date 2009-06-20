using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Generates a bound function body where all names have been resolved.
    /// </summary>
    public class FunctionBinder : IUnboundExprVisitor<IBoundExpr>
    {
        public static void Bind(BindingContext context, Function function)
        {
            if (function.Type.Return == null) throw new InvalidOperationException("Can only bind functions whose type is already bound.");

            var scope = new Scope();

            // create a local slot for the arg if there is one
            if (function.Type.Parameter.Unbound != Decl.Unit)
            {
                scope.Define("__arg", Decl.Unit /* ignored */, false);
            }

            var binder = new FunctionBinder(function, context, scope);

            // bind the function
            function.Bind(binder);

            // make sure declared return type matches actual return type
            if (!DeclComparer.TypesMatch(function.Type.Return.Bound, function.Body.Bound.Type))
            {
                if (function.Body.Bound.Type == Decl.EarlyReturn)
                {
                    //### bob: should find the return expr
                    throw new CompileException(function.Position, "Unneeded explicit \"return\".");
                }
                else
                {
                    throw new CompileException(function.Position, String.Format("{0} is declared to return {1} but is returning {2}.",
                        function.Name, function.Type.Return.Bound, function.Body.Bound.Type));
                }
            }
        }

        public Scope Scope { get; private set; }

        #region IUnboundExprVisitor<IBoundExpr> Members

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(CallExpr expr)
        {
            var boundArg = expr.Arg.Accept(this);

            var namedTarget = expr.Target as NameExpr;
            if (namedTarget != null)
            {
                return mContext.Compiler.ResolveName(mFunction, Scope, namedTarget.Position,
                    namedTarget.Name, namedTarget.TypeArgs, boundArg);
            }

            IBoundExpr target = expr.Target.Accept(this);

            // see if we're calling a function
            FuncType funcType = target.Type as FuncType;
            if (funcType != null)
            {
                // check that args match
                if (!DeclComparer.TypesMatch(funcType.Parameter.Bound, boundArg.Type))
                {
                    throw new CompileException(expr.Position, "Argument types passed to evaluated function reference do not match function's parameter types.");
                }

                // simply apply the arg to the bound expression
                return new BoundCallExpr(target, boundArg);
            }

            // not calling a function, so try to desugar to a __Call
            var callArg = new BoundTupleExpr(new IBoundExpr[] { target, boundArg });

            var call = mContext.Compiler.ResolveFunction(mFunction, expr.Target.Position,
                "__Call", new IUnboundDecl[0], callArg);

            if (call != null) return call;

            throw new CompileException(expr.Position, "Target of call is not a function.");
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(ArrayExpr expr)
        {
            var elementType = (IBoundDecl)null;

            if (expr.ElementType != null)
            {
                elementType = TypeBinder.Bind(mContext, expr.ElementType);
            }

            var elements = expr.Elements.Accept(this);

            // infer the type from the elements
            if (elementType == null)
            {
                var index = 0;
                foreach (var element in elements)
                {
                    if (elementType == null)
                    {
                        // take the type of the first
                        elementType = element.Type;
                    }
                    else
                    {
                        // make sure the others match
                        if (!DeclComparer.TypesMatch(elementType, element.Type))
                            throw new CompileException(expr.Position, String.Format("Array elements must all be the same type. Array is type {0}, but element {1} is type {2}.",
                                elementType, index, element.Type));
                    }

                    index++;
                }
            }

            return new BoundArrayExpr(elementType, elements, expr.IsMutable);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(AssignExpr expr)
        {
            var value = expr.Value.Accept(this);

            // handle a name target: foo <- 3
            var nameTarget = expr.Target as NameExpr;
            if (nameTarget != null)
            {
                // see if it's a local
                if (Scope.Contains(nameTarget.Name))
                {
                    if (!Scope[nameTarget.Name].IsMutable) throw new CompileException(expr.Position, "Cannot assign to immutable local.");

                    // direct assign to local
                    return new StoreExpr(new LocalsExpr(), Scope[nameTarget.Name], value);
                }

                // look for an assignment function
                return TranslateAssignment(nameTarget.Position, nameTarget.Name, nameTarget.TypeArgs, new UnitExpr(Position.None), value);
            }

            // handle a function apply target: Foo 1 <- 3  ==> Foo<- (1, 3)
            var callTarget = expr.Target as CallExpr;
            if (callTarget != null)
            {
                var callArg = callTarget.Arg.Accept(this);

                // see if it's a direct function call
                //### bob: there's a bug here. if the NameExpr is a local variable, translating to an assignment 
                // based on its name isn't right.
                var funcName = callTarget.Target as NameExpr;
                if ((funcName != null) && !mContext.Compiler.IsLocal(mFunction, Scope, funcName.Name))
                {
                    // translate the call
                    return TranslateAssignment(callTarget.Position, funcName.Name, funcName.TypeArgs, callArg, value);
                }

                // not calling a function, so try to desugar to a __Call<-
                var desugaredCallTarget = callTarget.Target.Accept(this);
                var desugaredCallArg = new BoundTupleExpr(new IBoundExpr[] { desugaredCallTarget, callArg, value });

                var call = mContext.Compiler.ResolveFunction(mFunction, expr.Target.Position,
                    "__Call<-", new IUnboundDecl[0], desugaredCallArg);

                if (call != null) return call;

                throw new CompileException(expr.Position, "Couldn't figure out what you're trying to do on the left side of an assignment.");
            }

            // handle an operator target: 1 $$ 2 <- 3  ==> $$<- (1, 2, 3)
            var operatorTarget = expr.Target as OperatorExpr;
            if (operatorTarget != null)
            {
                var opArg = new BoundTupleExpr(new IBoundExpr[]
                    { operatorTarget.Left.Accept(this),
                      operatorTarget.Right.Accept(this) });

                return TranslateAssignment(operatorTarget.Position, operatorTarget.Name, null /* no operator generics yet */, opArg, value);
            }

            var tupleTarget = expr.Target as TupleExpr;
            if (tupleTarget != null)
            {
                //### bob: need to handle tuple decomposition here:
                //         a, b <- (1, 2)
                throw new NotImplementedException();
            }

            // if we got here, it's not a valid assignment expression
            throw new CompileException(expr.Position, "Cannot assign to " + expr.Target);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(BlockExpr block)
        {
            // create an inner scope
            Scope.Push();

            var exprs = new List<IBoundExpr>();
            var index = 0;
            foreach (IUnboundExpr expr in block.Exprs)
            {
                var bound = expr.Accept(this);

                // all but last expression must be void
                if (index < block.Exprs.Count - 1)
                {
                    if (bound.Type != Decl.Unit) throw new CompileException(expr.Position, "All expressions in a block except the last must be of type Unit. " + block.ToString());
                }

                index++;

                exprs.Add(bound);
            }

            Scope.Pop();

            return new BoundBlockExpr(exprs);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(DefineExpr expr)
        {
            if (Scope.Contains(expr.Name)) throw new CompileException(expr.Position, "A local variable named \"" + expr.Name + "\" is already defined in this scope.");

            var value = expr.Value.Accept(this);

            // add it to the scope
            Scope.Define(expr.Name, value.Type, expr.IsMutable);

            // assign it
            return new StoreExpr(new LocalsExpr(), Scope[expr.Name], value);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(FuncRefExpr expr)
        {
            IBoundDecl paramType = null;
            if (expr.ParamType != null)
            {
                paramType = TypeBinder.Bind(mContext, expr.ParamType);
            }

            var callable = mContext.Compiler.Functions.Find(mContext.Compiler, mFunction.SearchSpace, 
                expr.Name.Name, expr.Name.TypeArgs, paramType);

            var function = callable as Function;

            //### bob: to support intrinsics, we'll need to basically create wrapper functions
            // that have the same type signature as the intrinsic and that do nothing but
            // call the intrinsic and return. then, we can get a reference to that wrapper.
            // 
            // to support foreign functions, we can either do the same thing, or change the
            // way function references work. if a function reference can be distinguished
            // between being a regular function, a foreign one (or later a closure), then
            // we can get rid of ForeignFuncCallExpr and just use CallExpr for foreign calls
            // too.
            if (function == null) throw new NotImplementedException("Can only get references to user-defined functions. Intrinsics, auto-generated, and foreign function references aren't supported yet.");

            return new BoundFuncRefExpr(function);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(IfThenExpr expr)
        {
            var bound = new BoundIfThenExpr(
                expr.Condition.Accept(this),
                expr.Body.Accept(this));

            if (bound.Condition.Type != Decl.Bool)
            {
                throw new CompileException(expr.Position, String.Format(
                    "Condition of if/then is returning type {0} but should be Bool.",
                    bound.Condition.Type));
            }

            if ((bound.Body.Type != Decl.Unit) && (bound.Body.Type != Decl.EarlyReturn))
            {
                throw new CompileException(expr.Position, String.Format(
                    "Body of if/then is returning type {0} but must be Unit (or a return) if there is no else branch.",
                    bound.Body.Type));
            }

            return bound;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(IfThenElseExpr expr)
        {
            var bound = new BoundIfThenElseExpr(
                expr.Condition.Accept(this),
                expr.ThenBody.Accept(this),
                expr.ElseBody.Accept(this));

            if (bound.Condition.Type != Decl.Bool)
            {
                throw new CompileException(expr.Position, String.Format(
                    "Condition of if/then/else is returning type {0} but should be Bool.",
                    bound.Condition.Type));
            }

            if (!DeclComparer.TypesMatch(bound.ThenBody.Type, bound.ElseBody.Type))
            {
                throw new CompileException(expr.Position, String.Format(
                    "Branches of if/then/else do not return the same type. Then arm returns {0} while else arm returns {1}.",
                    bound.ThenBody.Type, bound.ElseBody.Type));
            }

            return bound;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(NameExpr expr)
        {
            return mContext.Compiler.ResolveName(mFunction, Scope,
                expr.Position, expr.Name, expr.TypeArgs, null);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(OperatorExpr expr)
        {
            // an operator is just function application
            var apply = new CallExpr(new NameExpr(expr.Position, expr.Name), new TupleExpr(new IUnboundExpr[] { expr.Left, expr.Right }));

            return apply.Accept(this);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(TupleExpr expr)
        {
            return new BoundTupleExpr(expr.Fields.Accept(this));
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(IntExpr expr)
        {
            return expr;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(BoolExpr expr)
        {
            return expr;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(StringExpr expr)
        {
            return expr;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(UnitExpr expr)
        {
            return expr;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(ReturnExpr expr)
        {
            return new BoundReturnExpr(expr.Value.Accept(this));
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(WhileExpr expr)
        {
            var bound = new BoundWhileExpr(expr.Condition.Accept(this), expr.Body.Accept(this));

            if (bound.Condition.Type != Decl.Bool)
            {
                throw new CompileException(expr.Position, String.Format(
                    "Condition of while/do is returning type {0} but should be Bool.",
                    bound.Condition.Type));
            }

            if (bound.Body.Type != Decl.Unit)
            {
                throw new CompileException(expr.Position, String.Format(
                    "Body of while/do is returning type {0} but should be Unit.",
                    bound.Body.Type));
            }

            return bound;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(ForExpr expr)
        {
            // a for expression is basically syntactic sugar for a while expression
            // and one or more iterators. for example, the following:
            //
            // for foo <- bar do
            //     Print foo
            // end
            //
            // is equivalent to:
            //
            // def _fooIter <- Iterate bar
            // while MoveNext _fooIter do
            //     def foo <- Current _fooIter
            //     Print foo
            // end
            //
            // so, to bind a for expression, we just desugar it, then bind that.

            var topExprs = new List<IUnboundExpr>();
            var conditionExpr = (IUnboundExpr)null;
            var whileExprs = new List<IUnboundExpr>();

            // instantiate each iterator
            foreach (var iterator in expr.Iterators)
            {
                // note: the iterator variable includes a space to ensure it can't collide with a
                // user-defined variable.
                var createIterator = new CallExpr(new NameExpr(iterator.Position, "Iterate"), iterator.Iterator);
                topExprs.Add(new DefineExpr(iterator.Position, iterator.Name + " iter", createIterator, false));

                var condition = new CallExpr(new NameExpr(iterator.Position, "MoveNext"), new NameExpr(iterator.Position, iterator.Name + " iter"));
                if (conditionExpr == null)
                {
                    conditionExpr = condition;
                }
                else
                {
                    // combine with previous condition(s)
                    conditionExpr = new OperatorExpr(iterator.Position, conditionExpr, "&", condition);
                }

                var currentValue = new CallExpr(new NameExpr(iterator.Position, "Current"), new NameExpr(iterator.Position, iterator.Name + " iter"));
                whileExprs.Add(new DefineExpr(iterator.Position, iterator.Name, currentValue, false));
            }

            // create the while loop
            whileExprs.Add(expr.Body);
            var whileExpr = new WhileExpr(expr.Position,
                conditionExpr,
                new BlockExpr(whileExprs));
            topExprs.Add(whileExpr);

            // build the whole block
            var block = new BlockExpr(topExprs);

            // now bind the whole thing
            return block.Accept(this);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(MatchExpr expr)
        {
            // bind the match
            var match = expr.Match.Accept(this);

            // make sure the patterns are the right shape
            foreach (var matchCase in expr.Cases)
            {
                if (!CaseDeclMatcher.Matches(mContext.Compiler, match.Type, matchCase.Case))
                {
                    throw new CompileException(matchCase.Position,
                        String.Format("The match case {0} is not the right shape to match a value of type {1}.", matchCase, match.Type));
                }
            }

            throw new NotImplementedException();
        }

        #endregion

        private IBoundExpr TranslateAssignment(Position position, string baseName, IList<IUnboundDecl> typeArgs, IBoundExpr arg, IBoundExpr value)
        {
            var name = baseName + "<-";

            // add the value argument
            arg = arg.AppendArg(value);

            return mContext.Compiler.ResolveFunction(mFunction, position, name, typeArgs, arg);
        }

        private FunctionBinder(Function function, BindingContext context, Scope scope)
        {
            mFunction = function;
            mContext = context;
            Scope = scope;
        }

        private BindingContext mContext;
        private Function mFunction;
    }
}
