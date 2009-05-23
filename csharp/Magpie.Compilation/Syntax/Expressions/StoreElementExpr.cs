using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class StoreElementExpr : IBoundExpr
    {
        public IBoundExpr Array { get; private set; }
        public IBoundExpr Index { get; private set; }
        public IBoundExpr Value { get; private set; }

        public StoreElementExpr(IBoundExpr structure, IBoundExpr index, IBoundExpr value)
        {
            Array = structure;
            Index = index;
            Value = value;
        }

        public IBoundDecl Type { get { return Decl.Unit; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
