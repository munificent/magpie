using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class AssignExpr : IUnboundExpr
    {
        public Position Position { get; private set; }
        public IUnboundExpr Target { get; private set; }
        public IUnboundExpr Value { get; private set; }

        public AssignExpr(Position position, IUnboundExpr target, IUnboundExpr value)
        {
            Position = position;
            Target = target;
            Value = value;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Target = Target.AcceptTransformer(transformer);
            Value = Value.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        public override string ToString() { return String.Format("{0} <- {1}", Target, Value); }
    }
}
