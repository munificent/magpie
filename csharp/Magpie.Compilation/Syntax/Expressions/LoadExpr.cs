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

        public IBoundDecl Type { get; private set; }

        public LoadExpr(IBoundExpr structure, IBoundDecl type, int index)
        {
            if (structure == null) throw new ArgumentNullException("structure");
            if (type == null) throw new ArgumentNullException("type");

            Struct = structure;
            Type = type;
            Index = index;
        }

        public LoadExpr(IBoundExpr structure, Field field)
            : this(structure, field.Type.Bound, field.Index)
        {
        }

        public override string ToString()
        {
            return "load " + Struct.ToString() + "[" + Index + "]";
        }

        #region IBoundExpr Members

        TReturn IBoundExpr.Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
