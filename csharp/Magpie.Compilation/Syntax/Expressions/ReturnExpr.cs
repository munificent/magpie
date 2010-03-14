using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ReturnExpr<TExpr>
    {
        public TExpr Value { get; set; }

        public ReturnExpr(TExpr value)
        {
            Value = value;
        }
    }

    public class ReturnExpr : ReturnExpr<IUnboundExpr>, IUnboundExpr
    {
        public ReturnExpr(Position position, IUnboundExpr value)
            : base(value)
        {
            Position = position;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Value = Value.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        #endregion
    }

    public class BoundReturnExpr : ReturnExpr<IBoundExpr>, IBoundExpr
    {
        public BoundReturnExpr(IBoundExpr value)
            : base(value)
        {
        }

        #region IBoundExpr Members

        public IBoundDecl Type { get { return Decl.EarlyReturn; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
