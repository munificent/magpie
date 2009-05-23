using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// An expression in both its unbound and bound form.
    /// </summary>
    public class Expr
    {
        public IUnboundExpr Unbound { get; private set; }
        public IBoundExpr Bound { get; private set; }

        public Expr(IUnboundExpr unbound)
        {
            if (unbound == null) throw new ArgumentNullException("unbound");

            Unbound = unbound;
        }

        public Expr(IBoundExpr bound)
        {
            if (bound == null) throw new ArgumentNullException("bound");

            Bound = bound;
        }

        public void Bind(IBoundExpr bound)
        {
            if (bound == null) throw new ArgumentNullException("bound");

            Bound = bound;

            // discard the unbound one now. makes sure we're clear on what state we expect
            // the expression to be in.
            Unbound = null;
        }
    }
}
