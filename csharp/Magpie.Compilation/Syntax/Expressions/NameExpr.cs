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
        public TokenPosition Position { get; private set; }

        public string Name { get; private set; }
        public IList<Decl> TypeArgs { get { return mTypeArgs; } }

        public NameExpr(KeyValuePair<string, TokenPosition> pair)
            : this(pair.Value, pair.Key, null)
        {
        }

        public NameExpr(TokenPosition position, string name)
            : this(position, name, null)
        {
        }

        public NameExpr(KeyValuePair<string, TokenPosition> pair, IEnumerable<Decl> typeArgs)
            : this(pair.Value, pair.Key, typeArgs)
        {
        }

        public NameExpr(TokenPosition position, string name, IEnumerable<Decl> typeArgs)
        {
            Position = position;

            Name = name;

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
                return Name + "[" + mTypeArgs.JoinAll(", ") + "]";
            }
            else
            {
                return Name;
            }
        }

        private readonly List<Decl> mTypeArgs = new List<Decl>();
    }
}
