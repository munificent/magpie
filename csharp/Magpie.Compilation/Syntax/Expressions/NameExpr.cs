using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// An identifier, possibly with type arguments.
    /// </summary>
    /// <example>
    /// Foo
    /// Some-longer-name!
    /// AGeneric'Int
    /// another'(String, (Int, Bool), (Int -> String))
    /// </example>
    public class NameExpr : IUnboundExpr
    {
        public string Name { get { return mName; } }
        public IList<Decl> TypeArgs { get { return mTypeArgs; } }

        public NameExpr(string name) : this(name, null) { }

        public NameExpr(string name, IEnumerable<Decl> typeArgs)
        {
            mName = name;

            if (typeArgs != null)
            {
                mTypeArgs.AddRange(typeArgs);
            }
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString()
        {
            if (mTypeArgs.Count > 0)
            {
                return mName + "[" + mTypeArgs.JoinAll(", ") + "]";
            }
            else
            {
                return mName;
            }
        }

        private string mName;
        private readonly List<Decl> mTypeArgs = new List<Decl>();
    }
}
