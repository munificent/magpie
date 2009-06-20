using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ConstructExpr : IBoundExpr
    {
        public Struct Struct { get; private set; }
        public IBoundExpr Arg { get; private set; }

        public ConstructExpr(Struct structure, IBoundExpr arg)
        {
            Struct = structure;
            Arg = arg;
        }

        public IBoundDecl Type { get { return Struct; } }

        public override string ToString()
        {
            return "construct " + Struct.ToString() + " " + Arg.ToString();
        }

        #region IBoundExpr Members

        TReturn IBoundExpr.Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
