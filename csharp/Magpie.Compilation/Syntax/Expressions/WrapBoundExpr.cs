using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Unbound expression that wraps a bound expression. Used for expressions that are
    /// automatically created by the compiler in bound form, but that need to go through
    /// the binding process so they can be used in generics.
    /// </summary>
    public class WrapBoundExpr : IUnboundExpr
    {
        // no position needed for this. should never cause a compile error.
        public TokenPosition Position { get { return TokenPosition.None; } }

        public IBoundExpr Bound { get { return mBound; } }

        public WrapBoundExpr(IBoundExpr bound)
        {
            mBound = bound;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private IBoundExpr mBound;
    }
}
