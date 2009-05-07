using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LoadExpr : IBoundExpr
    {
        public IBoundExpr Struct;
        public int Index { get; private set; }

        public LoadExpr(IBoundExpr structure, Decl type, int index)
        {
            Struct = structure;
            Type = type;
            Index = index;
        }

        public LoadExpr(IBoundExpr structure, Field field)
            : this(structure, field.Type, field.Index)
        {
        }

        public Decl Type { get; private set; }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
