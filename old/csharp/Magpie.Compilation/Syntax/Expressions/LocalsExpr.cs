using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LocalsExpr : IBoundExpr
    {
        public IBoundDecl Type { get { throw new NotSupportedException(); } }

        public override string ToString()
        {
            return "_locals";
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
