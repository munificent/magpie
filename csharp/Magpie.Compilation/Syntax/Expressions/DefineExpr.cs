using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DefineExpr : IUnboundExpr
    {
        public Position Position { get; private set; }
        public string Name { get; private set; }
        public IUnboundExpr Value { get; private set; }
        public bool IsMutable { get; private set; }

        public DefineExpr(Position position, string name, IUnboundExpr value, bool isMutable)
        {
            Position = position;
            Name = name;
            Value = value;
            IsMutable = isMutable;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString()
        {
            return String.Format("{0} {1} <- {2}", IsMutable ? "mutable" : "def", Name, Value);
        }
    }
}
