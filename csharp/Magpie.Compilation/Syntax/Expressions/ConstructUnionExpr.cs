using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ConstructUnionExpr : IBoundExpr
    {
        public UnionCase Case { get { return mCase; } }
        public Union Union { get { return mCase.Union; } }

        public ConstructUnionExpr(UnionCase unionCase)
        {
            mCase = unionCase;
        }

        private UnionCase mCase;

        public Decl Type { get { return mCase.Union.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
