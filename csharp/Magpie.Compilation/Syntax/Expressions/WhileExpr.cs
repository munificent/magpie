using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Base class for an unbound or bound "while/do" expression.
    /// </summary>
    /// <typeparam name="TExpr"></typeparam>
    public abstract class WhileExpr<TExpr>
    {
        public TExpr Condition { get; private set; }
        public TExpr Body { get; private set; }

        public WhileExpr(TExpr condition, TExpr body)
        {
            Condition = condition;
            Body = body;
        }

        public override string ToString() { return String.Format("while {0} do {1}", Condition, Body); }
    }

    /// <summary>
    /// An unbound "while/do" expression.
    /// </summary>
    public class WhileExpr : WhileExpr<IUnboundExpr>, IUnboundExpr
    {
        public TokenPosition Position { get; private set; }

        public WhileExpr(TokenPosition position, IUnboundExpr condition, IUnboundExpr body) : base(condition, body)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    /// <summary>
    /// A bound "while/do" expression.
    /// </summary>
    public class BoundWhileExpr : WhileExpr<IBoundExpr>, IBoundExpr
    {
        public BoundWhileExpr(IBoundExpr condition, IBoundExpr body) : base(condition, body) { }

        public Decl Type { get { return Body.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
