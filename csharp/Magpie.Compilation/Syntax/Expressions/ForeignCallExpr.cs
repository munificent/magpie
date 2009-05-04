using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ForeignCallExpr : IBoundExpr
    {
        public ForeignFunction Function { get { return mFunction; } }
        public IBoundExpr Arg { get { return mArg; } }

        public ForeignCallExpr(ForeignFunction function, IBoundExpr arg)
        {
            mFunction = function;
            mArg = arg;
        }

        #region IBoundExpr Members

        Decl IBoundExpr.Type
        {
            get { return mFunction.FuncType.Return; }
        }

        TReturn IBoundExpr.Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion

        private ForeignFunction mFunction;
        private IBoundExpr mArg;
    }
}
