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
        public Position Position { get; private set; }

        public string Name { get; private set; }
        public IList<IUnboundDecl> TypeArgs { get { return mTypeArgs; } }

        public NameExpr(Position position, string name)
            : this(position, name, null)
        {
        }

        public NameExpr(Tuple<string, Position> pair, IEnumerable<IUnboundDecl> typeArgs)
            : this(pair.Item2, pair.Item1, typeArgs)
        {
        }

        public NameExpr(Position position, string name, IEnumerable<IUnboundDecl> typeArgs)
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

        private readonly List<IUnboundDecl> mTypeArgs = new List<IUnboundDecl>();
    }
}
