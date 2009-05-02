using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileUnit
    {
        public readonly List<string> Strings = new List<string>();
        public IList<BoundFunction> Bound { get { return mBound; } }

        public CompileUnit(IForeignStaticInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public int DefineString(string text)
        {
            // look it up in the string table
            int index = Strings.IndexOf(text);

            if (index == -1)
            {
                // new string, so add it
                Strings.Add(text);
                index = Strings.Count - 1;
            }

            return index;
        }

        public void Include(SourceFile file)
        {
            AddNamespace(file.Name, file, file);
        }

        public void Compile(Stream outputStream)
        {
            //### bob: ideally, should compile all functions, just for error-checking
            //         but only include reached ones
            // recursively bind the reached functions, starting with Main()
            Function main = mFunctions.Find(func => func.Name == "Main");
            FunctionBinder.Bind(this, main, null);

            BytecodeFile file = new BytecodeFile(this);
            file.Save(outputStream);
        }

        /// <summary>
        /// Gets whether the given function call matches the function.
        /// </summary>
        /// <param name="name">Fully-qualified name of the function being called.</param>
        /// <param name="function">The function being matched.</param>
        /// <param name="typeArgs">The explicit type arguments applied.</param>
        /// <param name="argTypes">Types of the arguments passed to it.</param>
        public bool Matches(Function function, string name, IList<Decl> typeArgs, Decl[] argTypes)
        {
            if (name != function.FullName) return false;

            // type arguments must match (if there are any)
            bool funcHasTypeArgs = (function.TypeParameters != null) && (function.TypeParameters.Count > 0);
            bool matchHasTypeArgs = (typeArgs != null) && (typeArgs.Count > 0);

            if (funcHasTypeArgs != matchHasTypeArgs) return false;
            if (funcHasTypeArgs && (function.TypeParameters.Count != typeArgs.Count)) return false;

            if (!DeclComparer.Equals(typeArgs.ToArray(), function.TypeParameters.ToArray())) return false;

            // argument types must match parameter types
            return DeclComparer.Equals(function.FuncType.ParameterTypes, argTypes);
        }

        public IBoundExpr AppendArg(IBoundExpr arg, IBoundExpr value)
        {
            if (arg is UnitExpr)
            {
                // no arg, so just use the value
                return value;
            }
            
            BoundTupleExpr tuple = arg as BoundTupleExpr;
            if (tuple != null)
            {
                // multiple args, so just add another
                BoundTupleExpr newArg = new BoundTupleExpr(tuple.Fields);
                newArg.Fields.Add(value);
                return newArg;
            }

            // single arg, so create a tuple
            return new BoundTupleExpr(new IBoundExpr[] { arg, value });
        }

        public IBoundExpr PrependArg(IBoundExpr arg, IBoundExpr value)
        {
            if (arg is UnitExpr)
            {
                // no arg, so just use the value
                return value;
            }

            BoundTupleExpr tuple = arg as BoundTupleExpr;
            if (tuple != null)
            {
                // multiple args, so just add another
                BoundTupleExpr newArg = new BoundTupleExpr(tuple.Fields);
                newArg.Fields.Insert(0, value);
                return newArg;
            }

            // single arg, so create a tuple
            return new BoundTupleExpr(new IBoundExpr[] { value, arg });
        }

        /// <summary>
        /// Resolves and binds a reference to a name.
        /// </summary>
        /// <param name="scope">The scope in which the name is being bound.</param>
        /// <param name="name">The name being resolved. May or may not be fully-qualified.</param>
        /// <param name="typeArgs">The type arguments being applied to the name. For
        /// example, resolving "foo[int, bool]" would pass in {int, bool} here.</param>
        /// <param name="arg">The argument being applied to the name.</param>
        /// <returns></returns>
        public IBoundExpr ResolveName(Function containingType, Function instancingContext, Scope scope, string name, IList<Decl> typeArgs, IBoundExpr arg)
        {
            Decl[] argTypes = null;
            if (arg != null) argTypes = arg.Type.Expanded;

            // see if it's a local
            if (scope.Contains(name))
            {
                if ((typeArgs != null) && (typeArgs.Count > 0)) throw new InvalidOperationException("Cannot apply generic type arguments to a local variable.");

                Field local = scope[name];

                // if the local is holding a function reference and we're passed args, call it
                if ((local.Type is FuncType) && (argTypes != null))
                {
                    // check that args match
                    FuncType funcType = (FuncType)local.Type;
                    if (!DeclComparer.Equals(funcType.ParameterTypes, argTypes))
                    {
                        throw new CompileException("Argument types passed to local function reference do not match function's parameter types.");
                    }

                    return new BoundCallExpr(new LoadExpr(new LocalsExpr(), local), arg);
                }

                // just load the value
                return new LoadExpr(new LocalsExpr(), scope[name]);
            }

            // implicitly apply () as the argument if no other argument was provided.
            // note that we do this *after* checking for locals because locals aren't
            // implicitly applied. since most locals aren't functions anyway, it won't
            // matter in most cases, and in cases where a local holds a function, the
            // user will mostly likely want to treat that function like a value: return
            // it, pass it around, etc.
            if (arg == null)
            {
                arg = new UnitExpr();
                argTypes = arg.Type.Expanded;
            }

            //### bob: all of this iterating through lists is dumb. there should really be
            // a dictionary that maps unique names to values.

            // see if it's intrinsic
            IBoundExpr intrinsic = IntrinsicExpr.Find(name, arg);
            if (intrinsic != null) return intrinsic;

            //### bob: should really be doing this in ResolveFunction so that foreign functions
            // can be namespaced, but that'll require shifting some stuff around.
            // see if it's a foreign function
            ForeignFunction foreign = ResolveForeignFunction(BoundFunction.GetUniqueName(name, typeArgs, argTypes));
            if (foreign != null) return new ForeignCallExpr(foreign, arg);

            // see if it's something defined at the sourcefile level
            BoundFunction bound = ResolveFunction(containingType, instancingContext, scope, name, typeArgs, argTypes);
            if (bound != null)
            {
                return new BoundCallExpr(new BoundFuncRefExpr(bound), arg);
            }

            throw new CompileException(String.Format("Could not resolve name {0}.", BoundFunction.GetUniqueName(name, typeArgs, argTypes)));
        }

        private ForeignFunction ResolveForeignFunction(string uniqueName)
        {
            return mForeignInterface.FindFunction(uniqueName);
        }

        public BoundFunction ResolveFunction(Function containingType, Function instancingContext, Scope scope, string name, IList<Decl> typeArgs, Decl[] argTypes)
        {
            // try the name as-is
            BoundFunction bound = LookUpFunction(containingType, scope, name, typeArgs, argTypes);
            if (bound != null) return bound;

            // try the current function's namespace
            bound = LookUpFunction(containingType, scope, Namespace.Qualify(containingType.Namespace, name), typeArgs, argTypes);
            if (bound != null) return bound;

            // try each of the open namespaces
            IList<string> namespaces = containingType.SourceFile.UsingNamespaces;
            for (int i = namespaces.Count - 1; i >= 0; i--)
            {
                bound = LookUpFunction(containingType, scope, Namespace.Qualify(namespaces[i], name), typeArgs, argTypes);
                if (bound != null) return bound;
            }

            // try the instance context's namespaces
            if (instancingContext != null)
            {
                bound = LookUpFunction(containingType, scope, Namespace.Qualify(instancingContext.Namespace, name), typeArgs, argTypes);
                if (bound != null) return bound;

                namespaces = instancingContext.SourceFile.UsingNamespaces;
                for (int i = namespaces.Count - 1; i >= 0; i--)
                {
                    bound = LookUpFunction(containingType, scope, Namespace.Qualify(namespaces[i], name), typeArgs, argTypes);
                    if (bound != null) return bound;
                }
            }

            // not found
            return null;
        }

        private BoundFunction LookUpFunction(Function containingType, Scope scope, string fullName, IList<Decl> typeArgs, Decl[] argTypes)
        {
            string uniqueName = BoundFunction.GetUniqueName(fullName, typeArgs, argTypes);

            // try the already bound functions
            foreach (var bound in mBound)
            {
                if (bound.Matches(uniqueName)) return bound;
            }

            // try to bind a function or previously instanced generic
            foreach (var function in mFunctions)
            {
                BoundFunction bound = TryBind(uniqueName, function, null);
                if (bound != null) return bound;
            }

            // try to instantiate a generic
            foreach (var generic in mGenerics)
            {
                // names must match
                if (generic.FullName != fullName) continue;

                Function instance = Instantiate(generic, typeArgs, argTypes);

                if (instance != null)
                {
                    // don't instantiate it multiple times
                    mFunctions.Add(instance);

                    BoundFunction bound = TryBind(uniqueName, instance, containingType);
                    if (bound != null)
                    {
                        return bound;
                    }
                }
            }

            // couldn't find it
            return null;
        }

        /// <param name="uniqueName"></param>
        /// <param name="function"></param>
        /// <param name="instancingContext">When binding a function that is an instance of the generic,
        /// this will be the function whose body contains the function call that causes the generic to
        /// be instantiated.</param>
        /// <returns></returns>
        private BoundFunction TryBind(string uniqueName, Function function, Function instancingContext)
        {
            if (function.Matches(uniqueName))
            {
                return FunctionBinder.Bind(this, function, instancingContext);
            }

            return null;
        }

        private Function Instantiate(Function generic, IList<Decl> typeArgs, IEnumerable<Decl> argTypes)
        {
            // apply the type arguments to the parameters
            var applicator = TypeArgApplicator.Create(generic, typeArgs, argTypes);
            if (applicator == null) return null;

            FuncType funcType = TypeInstancer.Instance(applicator, generic.FuncType);
            if (funcType == null) return null;

            IUnboundExpr body = UnboundBodyInstancer.Instance(applicator, generic.Body);

            Function instance = new Function(generic.Name, typeArgs, funcType, body, applicator.CanInfer);

            instance.Qualify(generic);

            return instance;
        }

        public void AddFunction(Function function)
        {
            if (function.IsGeneric)
            {
                mGenerics.Add(function);
            }
            else
            {
                mFunctions.Add(function);
            }
        }

        public void AddBound(BoundFunction bound)
        {
            mBound.Add(bound);
        }

        private void AddNamespace(string parentName, SourceFile file, Namespace namespaceObj)
        {
            foreach (Function function in namespaceObj.Functions)
            {
                function.Qualify(parentName, file);
                AddFunction(function);
            }

            var auto = new AutoFunctions(this);

            foreach (Struct structure in namespaceObj.Structs)
            {
                structure.Qualify(parentName, file);
                auto.BuildFunctions(structure);
            }

            foreach (Union union in namespaceObj.Unions)
            {
                union.Qualify(parentName, file);
                auto.BuildFunctions(union);
            }

            foreach (Namespace childNamespace in namespaceObj.Namespaces)
            {
                string name = Namespace.Qualify(parentName, childNamespace.Name);
                AddNamespace(name, file, childNamespace);
            }
        }

        private readonly List<BoundFunction> mBound = new List<BoundFunction>();
        private readonly List<Function> mGenerics = new List<Function>();
        private readonly List<Function> mFunctions = new List<Function>();
        private readonly IForeignStaticInterface mForeignInterface;
    }
}
