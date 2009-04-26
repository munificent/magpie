using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ApplyExpr<TExpr>
    {
        public TExpr Target;
        public TExpr Arg;

        public ApplyExpr(TExpr target, TExpr arg)
        {
            Target = target;
            Arg = arg;
        }

        public override string ToString() { return String.Format("{0} {1}", Target, Arg); }
    }

    public class ApplyExpr : ApplyExpr<IUnboundExpr>, IUnboundExpr
    {
        public ApplyExpr(IUnboundExpr target, IUnboundExpr arg) : base(target, arg) {}

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return String.Format("{0} {1}", Target, Arg); }
    }

    public class BoundApplyExpr : ApplyExpr<IBoundExpr>, IBoundExpr
    {
        public BoundApplyExpr(IBoundExpr target, IBoundExpr arg) : base(target, arg) { }

        public Decl Type
        {
            get { return ((FuncType)Target.Type).Return; }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
