using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LetExpr : IUnboundExpr
    {
        /// <summary>
        /// Gets the name of the value that will be defined if the condition succeeds.
        /// </summary>
        public string Name { get; private set; }

        public IUnboundExpr Condition { get; private set; }
        public IUnboundExpr ThenBody { get; private set; }
        public IUnboundExpr ElseBody { get; private set; }

        public LetExpr(Position position, string name, IUnboundExpr condition,
            IUnboundExpr thenBody, IUnboundExpr elseBody)
        {
            Position = position;
            Name = name;
            Condition = condition;
            ThenBody = thenBody;
            ElseBody = elseBody;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        TReturn IUnboundExpr.Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
