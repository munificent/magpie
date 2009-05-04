using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: this type is used for both instantiated and generic types (Foo'A and Foo'Int). Things might be
    // a little clearer if that was split out the way bound and unbound expressions are.
    /// <summary>
    /// Base class for a user-defined named type.
    /// </summary>
    public abstract class TypeDefinition
    {
        public string Name { get { return mName; } }
        public string Namespace { get { return mNamespace; } }

        /// <summary>
        /// Gets the source file this type is defined in. Used to determine which namespaces are in
        /// use (i.e the set of "using" directives) when binding this definition.
        /// </summary>
        public SourceFile SourceFile { get { return mSourceFile; } }

        public string FullName { get { return Compiler.QualifyName(Namespace, Name); } }

        public bool IsGeneric { get { return mTypeParams.Count > 0; } }

        public virtual Decl Type { get { return new NamedType(mName, TypeParameters); } }

        /// <summary>
        /// For a type instanced by explicitly applying type arguments to a generic, gets the
        /// list of type arguments applied. Will be null for generic types.
        /// </summary>
        public IList<Decl> TypeParameters { get { return mTypeParams; } }

        public override string ToString()
        {
            return String.Format("{0}{1}",
                Name,
                IsGeneric ? ("[" + TypeParameters.JoinAll(", ") + "]") : "");
        }

        /// <summary>
        /// Stores the list of concrete argument types explicitly applied to instance a generic into
        /// this function.
        /// </summary>
        /// <param name="args">The type arguments.</param>
        public void BindTypeArgs(IEnumerable<Decl> args)
        {
            mTypeParams = new List<Decl>(args);
        }

        public void Qualify(string namespaceName, SourceFile sourceFile)
        {
            mNamespace = namespaceName;
            mSourceFile = sourceFile;

            OnQualify();
        }

        public void Qualify(TypeDefinition parentType)
        {
            Qualify(parentType.Namespace, parentType.SourceFile);
        }

        protected TypeDefinition(string name, IEnumerable<Decl> typeParams)
        {
            mName = name;

            if (typeParams != null)
            {
                mTypeParams.AddRange(typeParams);
            }
        }

        protected virtual void OnQualify() { }

        private string mName;
        private List<Decl> mTypeParams = new List<Decl>();

        private string mNamespace = String.Empty;
        private SourceFile mSourceFile;
    }
}
