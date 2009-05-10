using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class IfThenElseExpr<TExpr>
    {
        public TExpr Condition;
        public TExpr ThenBody;
        public TExpr ElseBody;

        public IfThenElseExpr(TExpr condition, TExpr thenBody, TExpr elseBody)
        {
            Condition = condition;
            ThenBody = thenBody;
            ElseBody = elseBody;
        }

        public override string ToString() { return String.Format("if {0} then {1} else {2}", Condition, ThenBody, ElseBody); }
    }

    public class IfThenElseExpr : IfThenElseExpr<IUnboundExpr>, IUnboundExpr
    {
        public IfThenElseExpr(IUnboundExpr condition, IUnboundExpr thenBody, IUnboundExpr elseBody) : base(condition, thenBody, elseBody) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundIfThenElseExpr : IfThenElseExpr<IBoundExpr>, IBoundExpr
    {
        public BoundIfThenElseExpr(IBoundExpr condition, IBoundExpr thenBody, IBoundExpr elseBody) : base(condition, thenBody, elseBody) { }

        public Decl Type { get { return ((IBoundExpr)ThenBody).Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
