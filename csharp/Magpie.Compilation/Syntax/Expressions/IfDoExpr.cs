using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class IfDoExpr<TExpr>
    {
        public TExpr Condition;
        public TExpr Body;

        public IfDoExpr(TExpr condition, TExpr body)
        {
            Condition = condition;
            Body = body;
        }

        public override string ToString() { return String.Format("if {0} do {1}", Condition, Body); }
    }

    public class IfDoExpr : IfDoExpr<IUnboundExpr>, IUnboundExpr
    {
        public IfDoExpr(IUnboundExpr condition, IUnboundExpr body) : base(condition, body) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return String.Format("if {0} do {1}", Condition, Body); }
    }
    public class BoundIfDoExpr : IfDoExpr<IBoundExpr>, IBoundExpr
    {
        public BoundIfDoExpr(IBoundExpr condition, IBoundExpr body) : base(condition, body) { }

        public Decl Type { get { return Decl.Unit; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
