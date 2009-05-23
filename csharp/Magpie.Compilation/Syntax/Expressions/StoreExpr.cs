using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class StoreExpr : IBoundExpr
    {
        public IBoundExpr Struct { get; private set; }
        public Field Field { get; private set; }
        public IBoundExpr Value { get; private set; }

        public StoreExpr(IBoundExpr structure, Field field, IBoundExpr value)
        {
            Struct = structure;
            Field = field;
            Value = value;
        }

        //### bob: this means assignment can't be used in the middle of an expression like
        //    you can in C. it might be nice if there was an alternate assignment syntax
        //    that did the assignment and also returned the assigned value.
        public IBoundDecl Type { get { return Decl.Unit; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
