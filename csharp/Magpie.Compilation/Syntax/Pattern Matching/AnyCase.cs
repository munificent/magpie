using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class AnyCase : ICaseExpr
    {
        public override string ToString()
        {
            return "_";
        }

        #region ICaseExpr Members

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
