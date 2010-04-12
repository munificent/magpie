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
                scope.Define(context.NameGenerator.Generate(), Decl.Unit /* ignored */, false);
            }

            var binder = new FunctionBinder(function, context, scope);

            // bind the function
            function.Bind(context, binder);

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

            //### bob: handle array constructors. hack! should be intrinsic
            if ((namedTarget != null) && (namedTarget.Name == "ArrayOf"))
            {
                // handle ArrayOf[Int]
                if ((expr.Arg is UnitExpr) && (namedTarget.TypeArgs.Count == 1))
                {
                    return new ArrayExpr(namedTarget.Position, namedTarget.TypeArgs[0]).Accept(this);
                }

                // handle ArrayOf (1, 2, 3)
                var elements = (IEnumerable<IUnboundExpr>)(new IUnboundExpr[] { expr.Arg });
                if (expr.Arg is TupleExpr)
                {
                    elements = ((TupleExpr)expr.Arg).Fields;
                }

                return new ArrayExpr(namedTarget.Position, elements).Accept(this);
            }

            var boundArg = expr.Arg.Accept(this);

            // see if we're accessing a record field
            BoundRecordType recordType = boundArg.Type as BoundRecordType;
            if ((namedTarget != null) &&
                (recordType != null) &&
                recordType.Fields.ContainsKey(namedTarget.Name))
            {
                // find the index of the field
                //### bob: ToList() here is a gross hack.
                var index = recordType.Fields.Keys.ToList().IndexOf(namedTarget.Name);

                // bind it
                return new LoadExpr(boundArg, recordType.Fields[namedTarget.Name], index);
            }

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
                new BoundArrayType(elementType));
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(AssignExpr expr)
        {
            var value = expr.Value.Accept(this);

            if (expr.Target is TupleExpr) throw new NotSupportedException("Assignment to a tuple must be simplified before binding.");

            // handle a name target: foo <- 3
            var nameTarget = expr.Target as NameExpr;
            if (nameTarget != null)
            {
                // see if it's a local
                if (Scope.Contains(nameTarget.Name))
                {
                    if (!Scope.IsMutable(nameTarget.Name)) throw new CompileException(expr.Position, "Cannot assign to immutable local.");

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

            // if we got here, it's not a valid assignment expression
            throw new CompileException(expr.Position, "Cannot assign to " + expr.Target);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(BlockExpr block)
        {
            // create an inner scope
            if (block.HasScope) Scope.Push();

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

            if (block.HasScope) Scope.Pop();

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
                    var temp = mContext.NameGenerator.Generate();
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
            string name = mContext.NameGenerator.Generate();

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
            throw new NotSupportedException("Let expressions should be desugared to more primitive expressions before binding.");
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(NameExpr expr)
        {
            //### bob: handle array constructors. hack! should be intrinsic
            if (expr.Name == "ArrayOf")
            {
                // handle ArrayOf[Int]
                if (expr.TypeArgs.Count == 1)
                {
                    return new ArrayExpr(expr.Position, expr.TypeArgs[0]).Accept(this);
                }
            }

            return mContext.ResolveName(mFunction, Scope,
                expr.Position, expr.Name, expr.TypeArgs, null);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(RecordExpr expr)
        {
            // bind the fields
            var fields = new Dictionary<string, IBoundExpr>();
            foreach (var field in expr.Fields)
            {
                fields.Add(field.Key, field.Value.Accept(this));
            }

            // determine the record type
            var fieldTypes = new Dictionary<string, IBoundDecl>();
            foreach (var field in fields)
            {
                fieldTypes.Add(field.Key, field.Value.Type);
            }

            var boundType = new BoundRecordType(fieldTypes);

            // discard the names and convert to just a struct
            // note that this assumes the fields will be correctly
            // iterated in sorted order
            return new BoundTupleExpr(fields.Values, boundType);
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
            throw new NotSupportedException("Loop expressions should be desugared to simpler expressions before binding.");
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(SyntaxExpr expr)
        {
            var innerExpr = SyntaxLiteral.Desugar(expr.Expr);
            return innerExpr.Accept(this);
        }

        IBoundExpr IUnboundExprVisitor<IBoundExpr>.Visit(MatchExpr expr)
        {
            // bind the value expression
            var boundValue = expr.Value.Accept(this);

            // convert the patterns to desugared conditional expressions
            var desugaredMatch = PatternMatcher.Match(mContext, expr, boundValue);

            // bind the desugared form
            return desugaredMatch.Accept(this);
        }

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

        private BindingContext mContext;
        private Function mFunction;
    }
}
