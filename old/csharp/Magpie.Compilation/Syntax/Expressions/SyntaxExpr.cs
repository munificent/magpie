using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A syntax literal expression.
    /// </summary>
    public class SyntaxExpr : IUnboundExpr
    {
        public IUnboundExpr Expr { get; private set; }

        public SyntaxExpr(Position position, IUnboundExpr expr)
        {
            Position = position;
            Expr = expr;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            //### bob: should the expr be transformed in a syntax literal?
            Expr = Expr.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        #endregion
    }
}
