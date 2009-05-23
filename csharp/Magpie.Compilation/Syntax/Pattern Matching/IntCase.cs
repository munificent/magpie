using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class IntCase : ICaseExpr
    {
        public int Value { get; private set; }

        public IntCase(int value)
        {
            Value = value;
        }

        public override string ToString()
        {
            return Value.ToString();
        }

        #region ICaseExpr Members

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
