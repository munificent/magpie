using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Compiler
    {
        public FunctionTable Functions { get { return mFunctions; } }
        public TypeTable Types { get { return mTypes; } }

        public Compiler(IForeignStaticInterface foreignInterface)
        {
            mForeignInterface = foreignInterface;
        }

        public void AddSourceFile(string filePath)
        {
            // defer the actual parsing until we compile. this way errors that occur
            // during source translation can generate compile errors at an expected
            // time.
            mSourcePaths.Add(filePath);
        }

        public IList<CompileError> Compile(Stream outputStream)
        {
            var errors = new List<CompileError>();

            try
            {
                // parse all the source files
                foreach (var path in mSourcePaths)
                {
                    SourceFile source = MagpieParser.ParseSourceFile(path);
                    AddNamespace(source.Name, source, source);
                }

                // build the function table
                mFunctions.Add(Intrinsic.All);

                // bind the user-defined types and create their auto-generated functions
                foreach (var structure in mTypes.Structs)
                {
                    TypeBinder.Bind(this, structure);
                    mFunctions.Add(structure.BuildFunctions());
                }

                foreach (var union in mTypes.Unions)
                {
                    TypeBinder.Bind(this, union);
                    mFunctions.Add(union.BuildFunctions());
                }

                foreach (var structure in mGenericStructs)
                {
                    mGenericFunctions.AddRange(structure.BuildFunctions());
                }

                foreach (var union in mGenericUnions)
                {
                    mGenericFunctions.AddRange(union.BuildFunctions());
                }

                if (mForeignInterface != null)
                {
                    mFunctions.Add(mForeignInterface.Functions.Cast<ICallable>());
                }

                mFunctions.BindAll(this);
            }
            catch (CompileException ex)
            {
                errors.Add(new CompileError(CompileStage.Compile, ex.Position.Line, ex.Message));
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
        /// example, resolving "foo'(int, bool)" would pass in {int, bool} here.</param>
        /// <param name="arg">The argument being applied to the name.</param>
        /// <returns></returns>
        public IBoundExpr ResolveName(Function function,
            Scope scope, TokenPosition position, string name,
            IList<IUnboundDecl> typeArgs, IBoundExpr arg)
        {
            IBoundDecl[] argTypes = null;
            if (arg != null) argTypes = arg.Type.Expand();

            IBoundExpr resolved = null;
            IBoundDecl resolvedType = null;

            // see if it's an argument
            ParamDecl param = function.Type.Parameters.Find(p => p.Name == name);
            if (param != null)
            {
                resolved = new LoadExpr(new LocalsExpr(), param.Type.Bound, 0);
                resolvedType = param.Type.Bound;

                if (function.Type.Parameters.Count > 1)
                {
                    // function has multiple parameters, so the first local slot is a tuple and we need to load the arg from it
                    byte argIndex = (byte)function.Type.Parameters.IndexOf(param);
                    resolved = new LoadExpr(resolved, param.Type.Bound, argIndex);
                }
            }
            else if (scope.Contains(name))
            {
                Field local = scope[name];

                // just load the value
                resolved = new LoadExpr(new LocalsExpr(), scope[name]);
                resolvedType = local.Type.Bound;
            }

            // if we resolved to a local name, handle it
            if (resolved != null)
            {
                // if the local or argument is holding a function reference and we're passed args, call it
                if (argTypes != null)
                {
                    FuncType funcType = resolvedType as FuncType;

                    // can only call functions
                    if (funcType == null) throw new CompileException(position, "Cannot call a local variable or argument that is not a function reference.");

                    // check that args match
                    if (!DeclComparer.TypesMatch(funcType.ParameterTypes, argTypes))
                    {
                        throw new CompileException(position, "Argument types passed to local function reference do not match function's parameter types.");
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
                argTypes = arg.Type.Expand();
            }

            // look up the function
            ICallable callable = FindFunction(function.SearchSpace, name, typeArgs, argTypes);
            if (callable == null) throw new CompileException(position, String.Format("Could not resolve name {0}.",
                FunctionTable.GetUniqueName(name, null, argTypes)));

            return callable.CreateCall(arg);
        }

        /// <summary>
        /// Looks for a function with the given name in all of the currently used namespaces.
        /// </summary>
        public ICallable FindFunction(NameSearchSpace searchSpace,
            string name, IList<IUnboundDecl> typeArgs, IBoundDecl[] argTypes)
        {
            foreach (var potentialName in searchSpace.SearchFor(name))
            {
                var bound = LookUpFunction(searchSpace, potentialName, typeArgs, argTypes);
                if (bound != null) return bound;
            }

            // not found
            return null;
        }

        public IBoundDecl FindType(NameSearchSpace searchSpace, TokenPosition position, string name,
            IEnumerable<IBoundDecl> typeArgs)
        {
            foreach (var potentialName in searchSpace.SearchFor(name))
            {
                var type = mTypes.Find(potentialName, typeArgs);
                if (type != null) return type;
            }

            // not found
            throw new CompileException(position, "Could not find a type named " + name + ".");
        }

        private ICallable LookUpFunction(NameSearchSpace searchSpace, string fullName,
            IList<IUnboundDecl> typeArgs, IBoundDecl[] argTypes)
        {
            var boundTypeArgs = TypeBinder.Bind(new BindingContext(this, searchSpace), typeArgs);

            string uniqueName = FunctionTable.GetUniqueName(fullName, boundTypeArgs, argTypes);

            // try the already bound functions
            ICallable callable;
            if (mFunctions.TryFind(fullName, boundTypeArgs, argTypes, out callable)) return callable;

            // try to instantiate a generic
            foreach (var generic in mGenericFunctions)
            {
                // names must match
                if (generic.Name != fullName) continue;

                ICallable instance = generic.Instantiate(this, boundTypeArgs, argTypes);

                //### bob: there's a bug here. it doesn't check that the *unique* names of the two functions
                // match, just the base names. i think this means it could incorrectly collide:
                // List'Int ()
                // List'Bool ()
                // but i'm not positive

                if (instance != null) return instance;
            }

            // couldn't find it
            return null;
        }

        private void AddNamespace(string parentName, SourceFile file, Namespace namespaceObj)
        {
            var searchSpace = new NameSearchSpace(parentName, file.UsingNamespaces);

            foreach (var function in namespaceObj.Functions)
            {
                function.SetSearchSpace(searchSpace);
                mFunctions.AddUnbound(function);
            }

            foreach (var structure in namespaceObj.Structs)
            {
                structure.SetSearchSpace(searchSpace);
                mTypes.Add(structure);
            }

            foreach (var union in namespaceObj.Unions)
            {
                union.SetSearchSpace(searchSpace);
                mTypes.Add(union);
            }

            foreach (var function in namespaceObj.GenericFunctions)
            {
                function.BaseType.SetSearchSpace(searchSpace);
                mGenericFunctions.Add(function);
            }

            foreach (var structure in namespaceObj.GenericStructs)
            {
                structure.BaseType.SetSearchSpace(searchSpace);
                mGenericStructs.Add(structure);
            }

            foreach (var union in namespaceObj.GenericUnions)
            {
                union.BaseType.SetSearchSpace(searchSpace);
                mGenericUnions.Add(union);
            }

            foreach (var childNamespace in namespaceObj.Namespaces)
            {
                var name = NameSearchSpace.Qualify(parentName, childNamespace.Name);
                AddNamespace(name, file, childNamespace);
            }
        }

        private readonly List<string> mSourcePaths = new List<string>();

        private readonly FunctionTable mFunctions = new FunctionTable();
        private readonly List<GenericStruct> mGenericStructs = new List<GenericStruct>();
        private readonly List<GenericUnion> mGenericUnions = new List<GenericUnion>();
        private readonly TypeTable mTypes = new TypeTable();
        private readonly List<IGenericCallable> mGenericFunctions = new List<IGenericCallable>();
        private readonly IForeignStaticInterface mForeignInterface;
    }
}
