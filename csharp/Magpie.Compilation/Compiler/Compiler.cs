using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Compiler
    {
        public static string QualifyName(string namespaceName, string name)
        {
            if (String.IsNullOrEmpty(namespaceName)) return name;
            return namespaceName + ":" + name;
        }

        public FunctionTable Functions { get { return mFunctions; } }

        public Compiler(IForeignStaticInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public void AddSourceFile(string filePath)
        {
            try
            {
                SourceFile source = MagpieParser.ParseSourceFile(filePath);
                AddNamespace(source.Name, source, source);
            }
            catch (Exception ex)
            {
                throw new Exception("Exception parsing \"" + filePath + "\".", ex);
            }
        }

        public IList<CompileError> Compile(Stream outputStream)
        {
            mFunctions.Add(Intrinsic.All);

            if (mForeignInterface != null)
            {
                mFunctions.Add(mForeignInterface.Functions);
            }

            var errors = new List<CompileError>();

            try
            {
                mFunctions.BindAll(this);
            }
            catch (CompileException ex)
            {
                errors.Add(new CompileError(CompileStage.Compiler, ex.Position.Line, ex.Message));
            }

            if (errors.Count == 0)
            {
                BytecodeFile file = new BytecodeFile(this);
                file.Save(outputStream);
            }

            return errors;
        }
        /// <summary>
        /// Resolves and binds a reference to a name.
        /// </summary>
        /// <param name="function">The function being compiled.</param>
        /// <param name="scope">The scope in which the name is being bound.</param>
        /// <param name="name">The name being resolved. May or may not be fully-qualified.</param>
        /// <param name="typeArgs">The type arguments being applied to the name. For
        /// example, resolving "foo[int, bool]" would pass in {int, bool} here.</param>
        /// <param name="arg">The argument being applied to the name.</param>
        /// <returns></returns>
        public IBoundExpr ResolveName(Function function, Function instancingContext,
            Scope scope, string name, IList<Decl> typeArgs, IBoundExpr arg)
        {
            Decl[] argTypes = null;
            if (arg != null) argTypes = arg.Type.Expanded;

            IBoundExpr resolved = null;
            Decl resolvedType = null;

            // see if it's an argument
            ParamDecl param = function.FuncType.Parameters.Find(p => p.Name == name);
            if (param != null)
            {
                resolved = new LoadExpr(new LocalsExpr(), param.Type, 0);
                resolvedType = param.Type;

                if (function.FuncType.Parameters.Count > 1)
                {
                    // function has multiple parameters, so the first local slot is a tuple and we need to load the arg from it
                    byte argIndex = (byte)function.FuncType.Parameters.IndexOf(param);
                    resolved = new LoadExpr(resolved, param.Type, argIndex);
                }
            }
            else if (scope.Contains(name))
            {
                Field local = scope[name];

                // just load the value
                resolved = new LoadExpr(new LocalsExpr(), scope[name]);
                resolvedType = local.Type;
            }

            // if we resolved to a local name, handle it
            if (resolved != null)
            {
                if ((typeArgs != null) && (typeArgs.Count > 0)) throw new CompileException("Cannot apply generic type arguments to a local variable or function argument.");

                // if the local or argument is holding a function reference and we're passed args, call it
                if (argTypes != null)
                {
                    FuncType funcType = resolvedType as FuncType;

                    // can only call functions
                    if (funcType == null) throw new CompileException("Cannot call a local variable or argument that is not a function reference.");

                    // check that args match
                    if (!DeclComparer.TypesMatch(funcType.ParameterTypes, argTypes))
                    {
                        throw new CompileException("Argument types passed to local function reference do not match function's parameter types.");
                    }

                    // call it
                    resolved = new BoundCallExpr(resolved, arg);
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
                arg = new UnitExpr(TokenPosition.None);
                argTypes = arg.Type.Expanded;
            }

            // look up the function
            ICallable callable = FindFunction(function, instancingContext, name, typeArgs, argTypes);
            if (callable == null) throw new CompileException(String.Format("Could not resolve name {0}.", FunctionTable.GetUniqueName(name, typeArgs, argTypes)));

            return callable.CreateCall(arg);
        }

        /// <summary>
        /// Looks for a function with the given name in all of the currently used namespaces.
        /// </summary>
        /// <param name="containingType"></param>
        /// <param name="instancingContext"></param>
        /// <param name="scope"></param>
        /// <param name="name"></param>
        /// <param name="typeArgs"></param>
        /// <param name="argTypes"></param>
        /// <returns></returns>
        public ICallable FindFunction(Function containingType, Function instancingContext,
            string name, IList<Decl> typeArgs, Decl[] argTypes)
        {
            // try the name as-is
            ICallable bound = LookUpFunction(containingType, name, typeArgs, argTypes);
            if (bound != null) return bound;

            // try the current function's namespace
            bound = LookUpFunction(containingType, QualifyName(containingType.Namespace, name), typeArgs, argTypes);
            if (bound != null) return bound;

            // try each of the open namespaces
            IList<string> namespaces = containingType.SourceFile.UsingNamespaces;
            for (int i = namespaces.Count - 1; i >= 0; i--)
            {
                bound = LookUpFunction(containingType, QualifyName(namespaces[i], name), typeArgs, argTypes);
                if (bound != null) return bound;
            }

            // try the instance context's namespaces
            if (instancingContext != null)
            {
                bound = LookUpFunction(containingType, QualifyName(instancingContext.Namespace, name), typeArgs, argTypes);
                if (bound != null) return bound;

                namespaces = instancingContext.SourceFile.UsingNamespaces;
                for (int i = namespaces.Count - 1; i >= 0; i--)
                {
                    bound = LookUpFunction(containingType, QualifyName(namespaces[i], name), typeArgs, argTypes);
                    if (bound != null) return bound;
                }
            }

            // not found
            return null;
        }

        private ICallable LookUpFunction(Function containingType, string fullName, IList<Decl> typeArgs, Decl[] argTypes)
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
                string name = Compiler.QualifyName(parentName, childNamespace.Name);
                AddNamespace(name, file, childNamespace);
            }
        }

        private readonly FunctionTable mFunctions = new FunctionTable();
        private readonly List<Function> mGenerics = new List<Function>();
        private readonly IForeignStaticInterface mForeignInterface;
    }
}
