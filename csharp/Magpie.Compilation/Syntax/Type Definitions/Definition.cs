using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Base class for a named, defined function or type.
    /// </summary>
    public abstract class Definition
    {
        public Position Position { get; private set; }

        /// <summary>
        /// Gets the name of the type. Will include the namespace once the type has been fully-qualified.
        /// </summary>
        public string Name
        {
            get { return NameSearchSpace.Qualify(SearchSpace.Namespace, mName); }
        }

        public string BaseName { get { return mName; } }

        public NameSearchSpace SearchSpace { get; private set; }

        /// <summary>
        /// For types that are instantiated generics, this will contain the type arguments used to
        /// instantiate it. Otherwise, it will be an empty array.
        /// </summary>
        public IBoundDecl[] TypeArguments { get; private set; }

        public override string ToString()
        {
            if (TypeArguments == null) return Name;

            switch (TypeArguments.Length)
            {
                case 0: return Name;
                case 1: return Name + "'" + TypeArguments[0].ToString();
                default: return Name + "'(" + TypeArguments.JoinAll(", ") + ")";
            }
        }

        /// <summary>
        /// Binds the definition to the name context in which it is defined.
        /// </summary>
        /// <param name="searchSpace"></param>
        public void BindSearchSpace(NameSearchSpace searchSpace)
        {
            SearchSpace = searchSpace;
        }

        /// <summary>
        /// Binds the type arguments used to instantiate this structure.
        /// </summary>
        /// <param name="typeArgs"></param>
        public void BindTypeArguments(IEnumerable<IBoundDecl> typeArgs)
        {
            TypeArguments = typeArgs.ToArray();
        }

        protected Definition(Position position, string name, IEnumerable<IBoundDecl> typeArgs)
        {
            Position = position;
            mName = name;

            if (typeArgs == null)
            {
                TypeArguments = new IBoundDecl[0];
            }
            else
            {
                TypeArguments = typeArgs.ToArray();
            }
        }

        protected Definition(Position position, string name)
            : this (position, name, new IBoundDecl[0])
        {
        }

        private string mName;
    }
}
