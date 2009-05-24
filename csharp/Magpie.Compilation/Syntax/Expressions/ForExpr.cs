using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ForExpr : IUnboundExpr
    {
        public IList<NamedIterator> Iterators { get; private set; }
        public IUnboundExpr Body { get; private set; }

        public ForExpr(Position position, IList<NamedIterator> iterators, IUnboundExpr body)
        {
            Position = position;
            Iterators = iterators;
            Body = body;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    /// <summary>
    /// Represents a single "for a <- foo" clause in a for expression.
    /// </summary>
    public class NamedIterator
    {
        public Position Position { get; private set; }

        public string Name { get; private set; }
        public IUnboundExpr Iterator { get; private set; }

        public NamedIterator(Position position, string name, IUnboundExpr iterator)
        {
            Position = position;
            Name = name;
            Iterator = iterator;
        }
    }
}
