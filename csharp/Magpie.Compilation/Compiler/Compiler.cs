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
                mFunctions.AddRange(Intrinsic.All);
                mFunctions.AddRange(Intrinsic.AllGenerics);

                // bind the user-defined types and create their auto-generated functions
                foreach (var structure in mTypes.Structs)
                {
                    TypeBinder.Bind(this, structure);
                    mFunctions.AddRange(structure.BuildFunctions());
                }

                foreach (var union in mTypes.Unions)
                {
                    TypeBinder.Bind(this, union);
                    mFunctions.AddRange(union.BuildFunctions());
                }

                foreach (var structure in mGenericStructs)
                {
                    mFunctions.AddRange(structure.BuildFunctions());
                }

                foreach (var union in mGenericUnions)
                {
                    mFunctions.AddRange(union.BuildFunctions());
                }

                if (mForeignInterface != null)
                {
                    mFunctions.AddRange(mForeignInterface.Functions.Cast<ICallable>());
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
            Scope scope, Position position, string name,
            IList<IUnboundDecl> typeArgs, IBoundExpr arg)
        {
            IBoundDecl argType = null;
            if (arg != null) argType = arg.Type;

            IBoundExpr resolved = null;

            // see if it's an argument
            //### bob: port again :(
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

                    // can only call functions
                    if (funcType == null) throw new CompileException(position, "Cannot call a local variable or argument that is not a function reference.");

                    // check that args match
                    if (!DeclComparer.TypesMatch(funcType.Parameter.Bound, argType))
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
                arg = new UnitExpr(Position.None);
                argType = arg.Type;
            }

            // look up the function
            var callable = Functions.Find(this, function.SearchSpace, name, typeArgs, argType);

            if (callable == null) throw new CompileException(position, String.Format("Could not resolve name {0}.",
                Callable.UniqueName(name, null, argType)));

            return callable.CreateCall(arg);
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
                mFunctions.Add(function);
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
        private readonly IForeignStaticInterface mForeignInterface;
    }
}
