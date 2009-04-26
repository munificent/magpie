using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class BlockExpr<TExpr>
    {
        public readonly List<TExpr> Exprs = new List<TExpr>();

        public BlockExpr(IEnumerable<TExpr> exprs)
        {
            Exprs.AddRange(exprs);
        }

        public override string ToString()
        {
            return "\n  " + Exprs.JoinAll("\n  ") + "\nend";
        }
    }

    public class BlockExpr : BlockExpr<IUnboundExpr>, IUnboundExpr
    {
        public BlockExpr(IEnumerable<IUnboundExpr> exprs) : base(exprs) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundBlockExpr : BlockExpr<IBoundExpr>, IBoundExpr
    {
        public BoundBlockExpr(IEnumerable<IBoundExpr> exprs) : base(exprs) { }

        public Decl Type
        {
            get
            {
                // a block's type is the type of the last expression
                return Exprs[Exprs.Count - 1].Type;
            }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
