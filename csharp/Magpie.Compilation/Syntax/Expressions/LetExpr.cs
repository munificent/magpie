using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LetExpr : IUnboundExpr
    {
        /// <summary>
        /// Gets the names of the values that will be defined if the condition succeeds.
        /// </summary>
        public IList<string> Names { get; private set; }

        public IUnboundExpr Condition { get; private set; }
        public IUnboundExpr ThenBody { get; private set; }
        public IUnboundExpr ElseBody { get; private set; }

        public LetExpr(Position position, IEnumerable<string> names, IUnboundExpr condition,
            IUnboundExpr thenBody, IUnboundExpr elseBody)
        {
            Position = position;
            Names = new List<string>(names);
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

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Condition = Condition.AcceptTransformer(transformer);
            ThenBody = ThenBody.AcceptTransformer(transformer);
            ElseBody = (ElseBody == null) ? null : ElseBody.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        #endregion
    }
}
