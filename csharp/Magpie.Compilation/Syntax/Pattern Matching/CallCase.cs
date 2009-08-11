using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CallCase : ICaseExpr
    {
        public ICaseExpr Called { get; private set; }
        public ICaseExpr Arg { get; private set; }

        public CallCase(ICaseExpr called, ICaseExpr arg)
        {
            Called = called;
            Arg = arg;
        }

        public override string ToString()
        {
            return Called.ToString() + " " + Arg.ToString();
        }

        #region ICaseExpr Members

        public Position Position { get { return Called.Position; } }

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
