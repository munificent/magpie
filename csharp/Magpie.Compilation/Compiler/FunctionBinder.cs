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
                scope.Define(GenerateName(), Decl.Unit /* ignored */, false);
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
            var namedTarget = expr.Target as NameExpr;

            // see if it's a macro call before binding the arg
            if ((namedTarget != null) && (mContext.Compiler.MacroProcessor != null))
            {
                IUnboundExpr macroResult = mContext.Compiler.MacroProcessor.Process(namedTarget.Name, expr.Arg);

                // if it was a macro call, bind the result of it
                if (macroResult != null) return macroResult.Accept(this);
            }

            var boundArg = expr.Arg.Accept(this);

            if (namedTarget != null)
            {
                return mContext.ResolveName(mFunction, Scope, namedTarget.Position,
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

            // see if we're accessing a tuple field
            var tupleType = boundArg.Type as BoundTupleType;
            if ((tupleType != null) && (target.Type == Decl.Int))
            {
                var index = target as IntExpr;
                if (index == null) throw new CompileException(expr.Position, "Tuple fields can only be accessed using a literal index, not an int expression.");

                // make sure the field is in range
                if ((index.Value < 0) || (index.Value >= tupleType.Fields.Count))
                    throw new CompileException(expr.Position, String.Format("Cannot access field {0} because the tuple only has {1} fields.", index.Value, tupleType.Fields.Count));

                // bind it
                return new LoadExpr(boundArg, tupleType.Fields[index.Value], index.Value);
            }

            // not calling a function, so try to desugar to a __Call
            var callArg = new BoundTupleExpr(new IBoundExpr[] { target, boundArg });

            var call = mContext.ResolveFunction(mFunction, expr.Target.Position,
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

            // build a structure for the array
            var fields = new List<IBoundExpr>();
            fields.Add(new IntExpr(expr.Elements.Count));

            fields.AddRange(elements);

            return new BoundTupleExpr(fields,
                new BoundArrayType(elementType, expr.IsMutable));
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
                var funcName = callTarget.Target as NameExpr;
                if ((funcName != null) && !mContext.Compiler.IsLocal(mFunction, Scope, funcName.Name))
                {
                    // translate the call
                    return TranslateAssignment(callTarget.Position, funcName.Name, funcName.TypeArgs, callArg, value);
                }

                // not calling a function, so try to desugar to a __Call<-
                var desugaredCallTarget = callTarget.Target.Accept(this);
                var desugaredCallArg = new BoundTupleExpr(new IBoundExpr[] { desugaredCallTarget, callArg, value });

                var call = mContext.ResolveFunction(mFunction, expr.Target.Position,
                    "__Call<-", new IUnboundDecl[0], desugaredCallArg);

                if (call != null) return call;

                throw new CompileException(expr.Position, "Couldn't figure out what you're trying to do on the left side of an assignment.");
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
            var stores = new List<IBoundExpr>();

            foreach (var define in expr.Definitions)
            {
                foreach (var name in define.Names)
                {
                    if (Scope.Contains(name)) throw new CompileException(define.Position,
                        "A local variable named \"" + name + "\" is already defined in this scope.");
                }

                var value = define.Value.Accept(this);

                if (define.Names.Count > 1)
                {

                    // splitting a tuple
                    var tupleType = value.Type as BoundTupleType;

                    if (tupleType == null) throw new CompileException(define.Position,
                        "Cannot define multiple names if the value is not a tuple.");

                    if (tupleType.Fields.Count < define.Names.Count) throw new CompileException(define.Position,
                        "Cannot bind more names in a define than the tuple has fields.");

                    // define a temporary for the entire tuple expression
                    var temp = GenerateName();
                    Scope.Define(temp, value.Type, false);

                    // split out the fields
                    int field = 0;
                    foreach (var name in define.Names)
                    {
                        Scope.Define(name, tupleType.Fields[field], expr.IsMutable);

                        // assign it
                        var fieldValue = new LoadExpr(value, tupleType.Fields[field], field);
                        stores.Add(new StoreExpr(new LocalsExpr(), Scope[name], fieldValue));

                        field++;
                    }
                }
                else
                {
                    // just a single variable

                    // add it to the scope
                    Scope.Define(define.Names[0], value.Type, expr.IsMutable);

                    // assign it
                    stores.Add(new StoreExpr(new LocalsExpr(), Scope[define.Names[0]], value));
                }
            }

            return new BoundBlockExpr(stores);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(FuncRefExpr expr)
        {
            IBoundDecl paramType = null;
            if (expr.ParamType != null)
            {
                paramType = TypeBinder.Bind(mContext, expr.ParamType);
            }

            var callable = mContext.Compiler.Functions.Find(mContext, 
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

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(LocalFuncExpr expr)
        {
            // create a unique name for the local function
            string name = GenerateName();

            // create a reference to it
            var reference = new FuncRefExpr(expr.Position,
                new NameExpr(expr.Position, name),
                expr.Type.Parameter.Unbound);

            // lift the local function into a new top-level function
            var function = new Function(expr.Position, name,
                expr.Type, expr.ParamNames, expr.Body);

            function.BindSearchSpace(mContext.SearchSpace);

            // bind it
            mContext.Compiler.Functions.AddAndBind(mContext.Compiler, function);

            // return the reference to it
            return reference.Accept(this);

            //### bob: eventually, will need to handle closures
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(IfExpr expr)
        {
            var bound = new BoundIfExpr(
                expr.Condition.Accept(this),
                expr.ThenBody.Accept(this),
                (expr.ElseBody == null) ? null : expr.ElseBody.Accept(this));

            if (bound.Condition.Type != Decl.Bool)
            {
                throw new CompileException(expr.Position, String.Format(
                    "Condition of if/then/else is returning type {0} but should be Bool.",
                    bound.Condition.Type));
            }

            if (bound.ElseBody != null)
            {
                // has an else, so make sure both arms match
                if (!DeclComparer.TypesMatch(bound.ThenBody.Type, bound.ElseBody.Type))
                {
                    throw new CompileException(expr.Position, String.Format(
                        "Branches of if/then/else do not return the same type. Then arm returns {0} while else arm returns {1}.",
                        bound.ThenBody.Type, bound.ElseBody.Type));
                }
            }
            else
            {
                // no else, so make the then body doesn't return a value
                if ((bound.ThenBody.Type != Decl.Unit) && (bound.ThenBody.Type != Decl.EarlyReturn))
                {
                    throw new CompileException(expr.Position, String.Format(
                        "Body of if/then is returning type {0} but must be Unit (or a return) if there is no else branch.",
                        bound.ThenBody.Type));
                }
            }

            return bound;
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(LetExpr expr)
        {
            // a let expression desugars like this:
            //
            //      let a b <- Foo then Bar else Bang
            // 
            // becomes...
            //
            //      def let__ <- Foo
            //      if Some? let__ then
            //          def a b <- 0 SomeValue let__
            //          Bar
            //      else Bang

            // use a space so we can't collide with a user's variable
            var optionName = GenerateName();

            // def a__ <- Foo
            var defineOption = new DefineExpr(expr.Position, optionName, expr.Condition, false);

            // Some? a__
            var condition = new CallExpr(new NameExpr(expr.Position, "Some?"),
                                         new NameExpr(expr.Position, optionName));

            // def a <- SomeValue a__
            var getValue = new CallExpr(new NameExpr(expr.Position, "SomeValue"),
                                        new NameExpr(expr.Position, optionName));
            var defineValue = new DefineExpr(expr.Position, expr.Names, getValue, false);

            var thenBody = new BlockExpr(new IUnboundExpr[] { defineValue, expr.ThenBody });
            var ifThen = new IfExpr(expr.Position, condition, thenBody, expr.ElseBody);
            var block = new BlockExpr(new IUnboundExpr[] { defineOption, ifThen });

            // now bind the desugared expression
            return block.Accept(this);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(NameExpr expr)
        {
            return mContext.ResolveName(mFunction, Scope,
                expr.Position, expr.Name, expr.TypeArgs, null);
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

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(LoopExpr expr)
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

            // instantiate each clause
            foreach (var clause in expr.Clauses)
            {
                if (clause.IsWhile)
                {
                    if (conditionExpr == null)
                    {
                        conditionExpr = clause.Expression;
                    }
                    else
                    {
                        // combine with previous condition(s)
                        conditionExpr = new CallExpr(new NameExpr(clause.Position, "&"), new TupleExpr(conditionExpr, clause.Expression));
                    }
                }
                else
                {
                    var iterName = GenerateName();
                    var createIterator = new CallExpr(new NameExpr(clause.Position, "Iterate"), clause.Expression);
                    topExprs.Add(new DefineExpr(clause.Position, iterName, createIterator, false));

                    var condition = new CallExpr(new NameExpr(clause.Position, "MoveNext"), new NameExpr(clause.Position, iterName));
                    if (conditionExpr == null)
                    {
                        conditionExpr = condition;
                    }
                    else
                    {
                        // combine with previous condition(s)
                        conditionExpr = new CallExpr(new NameExpr(clause.Position, "&"), new TupleExpr(conditionExpr, condition));
                    }

                    var currentValue = new CallExpr(new NameExpr(clause.Position, "Current"), new NameExpr(clause.Position, iterName));
                    whileExprs.Add(new DefineExpr(clause.Position, clause.Name, currentValue, false));
                }
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

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(SyntaxExpr expr)
        {
            var innerExpr = SyntaxLiteral.Desugar(expr.Expr);
            return innerExpr.Accept(this);
        }

        /*
        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(MatchExpr expr)
        {
            // bind the value expression
            var boundValue = expr.Value.Accept(this);

            // convert the patterns to desugared conditional expressions
            var desugaredMatch = PatternMatcher.Match(mContext, expr, boundValue);

            // bind the desugared form
            return desugaredMatch.Accept(this);
        }
        */

        #endregion

        private IBoundExpr TranslateAssignment(Position position, string baseName, IList<IUnboundDecl> typeArgs, IBoundExpr arg, IBoundExpr value)
        {
            var name = baseName + "<-";

            // add the value argument
            arg = arg.AppendArg(value);

            return mContext.ResolveFunction(mFunction, position, name, typeArgs, arg);
        }

        private FunctionBinder(Function function, BindingContext context, Scope scope)
        {
            mFunction = function;
            mContext = context;
            Scope = scope;
        }

        //### bob: should refactor code to use this for all temps
        private static string GenerateName()
        {
            sTempIndex++;

            // using space in identifiers ensures it can't collide with a user-defined name
            return "__temp " + sTempIndex;
        }

        private BindingContext mContext;
        private Function mFunction;
        private static int sTempIndex;
    }
}
