using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ConstructUnionExpr : IBoundExpr
    {
        public UnionCase Case { get; private set; }
        public IBoundExpr Arg { get; private set; }

        public ConstructUnionExpr(UnionCase unionCase, IBoundExpr arg)
        {
            Case = unionCase;
            Arg = arg;
        }

        public IBoundDecl Type { get { return Case.Union; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
