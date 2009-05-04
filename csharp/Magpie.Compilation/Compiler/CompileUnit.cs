using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileUnit
    {
        public static string QualifyName(string namespaceName, string name)
        {
            if (String.IsNullOrEmpty(namespaceName)) return name;
            return namespaceName + ":" + name;
        }

        public FunctionTable BoundFunctions { get { return mFunctions; } }

        public CompileUnit(IForeignStaticInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public void Include(SourceFile file)
        {
            AddNamespace(file.Name, file, file);
        }

        public void Compile(Stream outputStream)
        {
            mFunctions.Add(Intrinsic.All);

            if (mForeignInterface != null)
            {
                mFunctions.Add(mForeignInterface.Functions);
            }

            mFunctions.BindAll(this);

            BytecodeFile file = new BytecodeFile(this);
            file.Save(outputStream);
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

            // look up the function
            ICallable callable = ResolveFunction(containingType, instancingContext, scope, name, typeArgs, argTypes);
            if (callable == null) throw new CompileException(String.Format("Could not resolve name {0}.", FunctionTable.GetUniqueName(name, typeArgs, argTypes)));

            return callable.CreateCall(arg);
        }

        public ICallable ResolveFunction(Function containingType, Function instancingContext, Scope scope, string name, IList<Decl> typeArgs, Decl[] argTypes)
        {
            // try the name as-is
            ICallable bound = LookUpFunction(containingType, scope, name, typeArgs, argTypes);
            if (bound != null) return bound;

            // try the current function's namespace
            bound = LookUpFunction(containingType, scope, QualifyName(containingType.Namespace, name), typeArgs, argTypes);
            if (bound != null) return bound;

            // try each of the open namespaces
            IList<string> namespaces = containingType.SourceFile.UsingNamespaces;
            for (int i = namespaces.Count - 1; i >= 0; i--)
            {
                bound = LookUpFunction(containingType, scope, QualifyName(namespaces[i], name), typeArgs, argTypes);
                if (bound != null) return bound;
            }

            // try the instance context's namespaces
            if (instancingContext != null)
            {
                bound = LookUpFunction(containingType, scope, QualifyName(instancingContext.Namespace, name), typeArgs, argTypes);
                if (bound != null) return bound;

                namespaces = instancingContext.SourceFile.UsingNamespaces;
                for (int i = namespaces.Count - 1; i >= 0; i--)
                {
                    bound = LookUpFunction(containingType, scope, QualifyName(namespaces[i], name), typeArgs, argTypes);
                    if (bound != null) return bound;
                }
            }

            // not found
            return null;
        }

        private ICallable LookUpFunction(Function containingType, Scope scope, string fullName, IList<Decl> typeArgs, Decl[] argTypes)
        {
            string uniqueName = FunctionTable.GetUniqueName(fullName, typeArgs, argTypes);

            // try the already bound functions
            ICallable callable;
            if (mFunctions.TryFind(fullName, argTypes, out callable)) return callable;

            // try to instantiate a generic
            foreach (var generic in mGenerics)
            {
                // names must match
                if (generic.FullName != fullName) continue;

                Function instance = Instantiate(generic, typeArgs, argTypes);

                if (instance != null)
                {
                    // don't instantiate it multiple times
                    BoundFunction bound = mFunctions.Add(instance);

                    if (instance.Matches(uniqueName))
                    {
                        FunctionBinder.Bind(this, bound, containingType);

                        return bound;
                    }
                }
            }

            // couldn't find it
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
            // shunt the generics over to the staging area. they will be instantiated
            // into real functions as needed.
            if (function.IsGeneric)
            {
                mGenerics.Add(function);
            }
            else
            {
                mFunctions.Add(function);
            }
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
                string name = CompileUnit.QualifyName(parentName, childNamespace.Name);
                AddNamespace(name, file, childNamespace);
            }
        }

        private readonly FunctionTable mFunctions = new FunctionTable();
        private readonly List<Function> mGenerics = new List<Function>();
        private readonly IForeignStaticInterface mForeignInterface;
    }
}
