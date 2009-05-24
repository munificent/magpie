using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class OperatorExpr : IUnboundExpr
    {
        public Position Position { get; private set; }
        public IUnboundExpr Left { get; private set; }
        public string Name { get; private set; }
        public IUnboundExpr Right { get; private set; }

        public OperatorExpr(Position position, IUnboundExpr left, string name, IUnboundExpr right)
        {
            Position = position;
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
