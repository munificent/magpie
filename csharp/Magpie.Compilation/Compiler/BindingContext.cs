using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Contains the context in which expression or declaration binding occurs.
    /// For example, the current name context for looking up names, the set
    /// of type arguments, etc.
    /// </summary>
    public class BindingContext
    {
        public Compiler Compiler { get; private set; }
        public NameSearchSpace SearchSpace { get; private set; }
        public IDictionary<string, IBoundDecl> TypeArguments { get; private set; }

        public BindingContext(Compiler compiler, NameSearchSpace searchSpace)
            : this(compiler, searchSpace, null, null)
        {
        }

        public BindingContext(Compiler compiler, NameSearchSpace searchSpace,
            IEnumerable<string> typeParameters, IEnumerable<IBoundDecl> typeArguments)
        {
            Compiler = compiler;
            SearchSpace = searchSpace;

            // build the argument dictionary
            TypeArguments = new Dictionary<string, IBoundDecl>();

            if ((typeParameters != null) && (typeArguments != null))
            {
                foreach (var pair in typeParameters.Zip(typeArguments))
                {
                    TypeArguments[pair.Item1] = pair.Item2;
                }
            }
        }

        /// <summary>
        /// Resolves and binds a reference to a name.
        /// </summary>
        /// <param name="function">The function being compiled.</param>
        /// <param name="scope">The scope in which the name is being bound.</param>
        /// <param name="name">The name being resolved. May or may not be fully-qualified.</param>
        /// <param name="typeArgs">The type arguments being applied to the name. For
        /// example, resolving "foo'(int, bool)" would pass in {int, bool} here.</param>
        /// <param name="arg">The argument being applied to the name.</param>
        /// <returns></returns>
        public IBoundExpr ResolveName(Function function,
            Scope scope, Position position, string name,
            IList<IUnboundDecl> typeArgs, IBoundExpr arg)
        {
            IBoundDecl argType = null;
            if (arg != null) argType = arg.Type;

            IBoundExpr resolved = null;

            // see if it's an argument
            if (function.ParamNames.Contains(name))
            {
                // load the argument
                resolved = new LoadExpr(new LocalsExpr(), function.ParameterType, 0);

                if (function.ParamNames.Count > 1)
                {
                    // function takes multiple parameters, so load it from the tuple
                    var paramTuple = (BoundTupleType)function.ParameterType;

                    var argIndex = (byte)function.ParamNames.IndexOf(name);
                    resolved = new LoadExpr(resolved, paramTuple.Fields[argIndex], argIndex);
                }
            }

            // see if it's a local
            if (scope.Contains(name))
            {
                var local = scope[name];

                // just load the value
                resolved = new LoadExpr(new LocalsExpr(), scope[name]);
            }

            // if we resolved to a local name, handle it
            if (resolved != null)
            {
                if (typeArgs.Count > 0) throw new CompileException(position, "Cannot apply type arguments to a local variable or function argument.");

                // if the local or argument is holding a function reference and we're passed args, call it
                if (argType != null)
                {
                    var funcType = resolved.Type as FuncType;

                    if (funcType != null)
                    {
                        // check that args match
                        if (!DeclComparer.TypesMatch(funcType.Parameter.Bound, argType))
                        {
                            throw new CompileException(position, "Argument types passed to local function reference do not match function's parameter types.");
                        }

                        // call it
                        resolved = new BoundCallExpr(resolved, arg);
                    }
                    else
                    {
                        // not calling a function, so try to desugar to a __Call
                        var callArg = new BoundTupleExpr(new IBoundExpr[] { resolved, arg });

                        resolved = ResolveFunction(function, position,
                            "__Call", new IUnboundDecl[0], callArg);

                        if (resolved == null) throw new CompileException(position, "Cannot call a local variable or argument that is not a function reference, and could not find a matching __Call.");
                    }
                }

                return resolved;
            }

            // implicitly apply () as the argument if no other argument was provided.
            // note that we do this *after* checking for locals because locals aren't
            // implicitly applied. since most locals aren't functions anyway, it won't
            // matter in most cases, and in cases where a local holds a function, the
            // user will mostly likely want to treat that function like a value: return
            // it, pass it around, etc.
            if (arg == null)
            {
                arg = new UnitExpr(Position.None);
                argType = arg.Type;
            }

            return ResolveFunction(function, position, name, typeArgs, arg);
        }

        public IBoundExpr ResolveFunction(Function function,
            Position position, string name,
            IList<IUnboundDecl> typeArgs, IBoundExpr arg)
        {
            if (arg == null) throw new ArgumentNullException("arg");

            // look up the function
            var callable = Compiler.Functions.Find(this, name, typeArgs, arg.Type);

            if (callable == null) throw new CompileException(position, String.Format("Could not resolve name {0}.",
                Callable.UniqueName(name, null, arg.Type)));

            return callable.CreateCall(arg);
        }
    }
}
