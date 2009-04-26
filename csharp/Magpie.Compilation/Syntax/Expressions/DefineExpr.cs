using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class DefineExpr : IUnboundExpr
    {
        public string Name;
        public IUnboundExpr Value;
        public bool IsMutable;

        public DefineExpr(string name, IUnboundExpr value, bool isMutable)
        {
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
