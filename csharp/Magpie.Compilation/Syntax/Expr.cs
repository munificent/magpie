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

        //### bob: for some reason, the binder doesn't work here. once that's fixed,
        // get rid of the explicit bound arg
        /// <summary>
        /// Binds this expression using the given binder to convert the unbound
        /// form to the bound one.
        /// </summary>
        /// <param name="binder">A binder to convert from unbound to bound form.</param>
        public void Bind(IUnboundExprVisitor<IBoundExpr> binder)
        {
            if (binder == null) throw new ArgumentNullException("binder");
            if (Unbound == null) throw new InvalidOperationException("Cannot bind an Expr that is already bound.");

            Bound = Unbound.Accept(binder);

            // discard the unbound one now. makes sure we're clear on what state we expect
            // the expression to be in.
            Unbound = null;
        }
    }
}
