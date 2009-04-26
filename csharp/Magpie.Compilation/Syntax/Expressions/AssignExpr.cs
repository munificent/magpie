using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class AssignExpr : IUnboundExpr
    {
        public IUnboundExpr Target;
        public IUnboundExpr Value;

        public AssignExpr(IUnboundExpr target, IUnboundExpr value)
        {
            Target = target;
            Value = value;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return String.Format("{0} <- {1}", Target, Value); }
    }
}
