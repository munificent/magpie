using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A named type declaration.
    /// </summary>
    public class NamedType : Decl
    {
        public string Name { get { return mName; } }
        public Decl[] TypeArgs { get { return mTypeArgs; } }

        public bool IsGeneric { get { return mTypeArgs.Length > 0; } }

        public NamedType(string name)
        {
            mName = name;
            mTypeArgs = new Decl[0];
        }

        public NamedType(string name, IEnumerable<Decl> typeArgs)
            : this(name)
        {
            mName = name;

            if (typeArgs != null)
            {
                mTypeArgs = typeArgs.ToArray();
            }
        }

        public override string ToString()
        {
            if (mTypeArgs.Length > 0) return mName + "[" + mTypeArgs.JoinAll(", ") + "]";

            return mName;
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private string mName;
        private Decl[] mTypeArgs;
    }
}
