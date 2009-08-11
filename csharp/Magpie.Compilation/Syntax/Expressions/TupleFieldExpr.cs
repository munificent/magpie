using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Expression for accessing a field in a tuple.
    /// </summary>
    public class TupleFieldExpr : IUnboundExpr
    {
        public Position Position { get { return Value.Position; } }

        /// <summary>
        /// Gets the expression resulting in a tuple whose field
        /// will be accessed.
        /// </summary>
        public IUnboundExpr Value { get; private set; }

        /// <summary>
        /// Gets the index of the field to access from the tuple.
        /// </summary>
        public int Field { get; private set; }

        public TupleFieldExpr(IUnboundExpr value, int field)
        {
            Value = value;
            Field = field;
        }


        #region IUnboundExpr Members

        TReturn IUnboundExpr.Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
