using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ConstructExpr : IBoundExpr
    {
        public Struct Struct { get { return mStruct; } }

        public ConstructExpr(Struct structure)
        {
            mStruct = structure;
        }

        private Struct mStruct;

        public Decl Type { get { return mStruct.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
