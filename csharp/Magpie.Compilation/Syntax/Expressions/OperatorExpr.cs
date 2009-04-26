using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class OperatorExpr : IUnboundExpr
    {
        public IUnboundExpr Left;
        public string Name;
        public IUnboundExpr Right;

        public OperatorExpr(IUnboundExpr left, string name, IUnboundExpr right)
        {
            Left = left;
            Name = name;
            Right = right;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return String.Format("{0} {1} {2}", Left, Name, Right); }
    }
}
