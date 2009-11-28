using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /*
    /// <summary>
    /// A pattern matching expression.
    /// </summary>
    public class MatchExpr : IUnboundExpr
    {
        /// <summary>
        /// The value being matched against.
        /// </summary>
        public IUnboundExpr Value { get; private set; }

        /// <summary>
        /// The cases the try to match, in order.
        /// </summary>
        public IList<MatchCase> Cases { get; private set; }

        public MatchExpr(Position position, IUnboundExpr value, IList<MatchCase> cases)
        {
            Position = position;
            Value = value;
            Cases = cases;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class MatchCase
    {
        public Position Position { get; private set; }
        public IPattern Pattern { get; private set; }
        public IUnboundExpr Body { get; private set; }

        public MatchCase(Position position, IPattern caseExpr, IUnboundExpr body)
        {
            Position = position;
            Pattern = caseExpr;
            Body = body;
        }
    }
    */
}
