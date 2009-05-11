using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class IfThenExpr<TExpr>
    {
        public TExpr Condition;
        public TExpr Body;

        public IfThenExpr(TExpr condition, TExpr body)
        {
            Condition = condition;
            Body = body;
        }

        public override string ToString() { return String.Format("if {0} then {1}", Condition, Body); }
    }

    public class IfThenExpr : IfThenExpr<IUnboundExpr>, IUnboundExpr
    {
        public TokenPosition Position { get; private set; }

        public IfThenExpr(TokenPosition position, IUnboundExpr condition, IUnboundExpr body)
            : base(condition, body)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return String.Format("if {0} then {1}", Condition, Body); }
    }
    public class BoundIfThenExpr : IfThenExpr<IBoundExpr>, IBoundExpr
    {
        public BoundIfThenExpr(IBoundExpr condition, IBoundExpr body) : base(condition, body) { }

        public Decl Type { get { return Decl.Unit; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
