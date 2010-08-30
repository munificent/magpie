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

        public IMacroProcessor MacroProcessor { get { return mMacroProcessor; } }

        //### bob: hack temp.
        public Action<string, long> FunctionStarted;

        public Compiler(IForeignStaticInterface foreignInterface, IMacroProcessor macroProcessor)
        {
            mForeignInterface = foreignInterface;
            mMacroProcessor = macroProcessor;

            //### bob: temp testing
            mMacroProcessor = new HackTempMacro();

            mFunctions = new FunctionTable(this);
            mTypes = new TypeTable(this);
        }

        public Compiler(IForeignStaticInterface foreignInterface)
            : this(foreignInterface, null)
        {
        }

        public Compiler()
            : this(null, null)
        {
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
                mTypes.BindAll();

                foreach (var structure in mTypes.Structs)
                {
                    mFunctions.AddRange(structure.BuildFunctions());
                }

                foreach (var union in mTypes.Unions)
                {
                    mFunctions.AddRange(union.BuildFunctions());
                }

                foreach (var structure in mTypes.GenericStructs)
                {
                    mFunctions.AddRange(structure.BuildFunctions());
                }

                foreach (var union in mTypes.GenericUnions)
                {
                    mFunctions.AddRange(union.BuildFunctions());
                }

                if (mForeignInterface != null)
                {
                    mFunctions.AddRange(mForeignInterface.Functions.Cast<ICallable>());
                }

                mFunctions.BindAll();
            }
            catch (CompileException ex)
            {
                errors.Add(new CompileError(CompileStage.Compile, ex.Position, ex.Message));
            }

            if (errors.Count == 0)
            {
                BytecodeFile file = new BytecodeFile(this);
                file.Save(outputStream);
            }

            return errors;
        }

        /// <summary>
        /// Gets whether or not the given name within the given function and scope
        /// represents a local variable (or function parameter).
        /// </summary>
        /// <param name="function">The function in whose body the name is being
        /// looked up.</param>
        /// <param name="scope">The current local variable scope.</param>
        /// <param name="name">The name to look up.</param>
        /// <returns><c>true</c> if the name is a local variable or function
        /// parameter.</returns>
        public bool IsLocal(Function function,
            Scope scope, string name)
        {
            // see if it's an argument
            if (function.ParamNames.Contains(name)) return true;

            // see if it's a local
            if (scope.Contains(name)) return true;

            return false;
        }

        private void AddNamespace(string parentName, SourceFile file, Namespace namespaceObj)
        {
            var searchSpace = new NameSearchSpace(parentName, file.UsingNamespaces);

            foreach (var function in namespaceObj.Functions)
            {
                function.BindSearchSpace(searchSpace);
                mFunctions.AddUnbound(function);
            }

            foreach (var structure in namespaceObj.Structs)
            {
                structure.BindSearchSpace(searchSpace);
                mTypes.Add(structure);
            }

            foreach (var union in namespaceObj.Unions)
            {
                union.BindSearchSpace(searchSpace);
                mTypes.Add(union);
            }

            foreach (var function in namespaceObj.GenericFunctions)
            {
                function.BaseType.BindSearchSpace(searchSpace);
                mFunctions.Add(function);
            }

            foreach (var structure in namespaceObj.GenericStructs)
            {
                structure.BaseType.BindSearchSpace(searchSpace);
                mTypes.Add(structure);
            }

            foreach (var union in namespaceObj.GenericUnions)
            {
                union.BaseType.BindSearchSpace(searchSpace);
                mTypes.Add(union);
            }

            foreach (var childNamespace in namespaceObj.Namespaces)
            {
                var name = NameSearchSpace.Qualify(parentName, childNamespace.Name);
                AddNamespace(name, file, childNamespace);
            }
        }

        private readonly List<string> mSourcePaths = new List<string>();

        private readonly FunctionTable mFunctions;
        private readonly TypeTable mTypes;
        private readonly IForeignStaticInterface mForeignInterface;
        private readonly IMacroProcessor mMacroProcessor;
    }
}
