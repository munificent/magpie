using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LoadExpr : IBoundExpr
    {
        public IBoundExpr Struct;
        public Field Field;

        public LoadExpr(IBoundExpr structure, Field field)
        {
            Struct = structure;
            Field = field;
        }

        public Decl Type { get { return Field.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
