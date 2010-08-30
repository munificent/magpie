using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: should get rid of these completely instead of having for loops desugar to this
    // and then compile it.
    /// <summary>
    /// Base class for an unbound or bound "while/do" expression.
    /// </summary>
    /// <typeparam name="TExpr"></typeparam>
    public abstract class WhileExpr<TExpr>
    {
        public TExpr Condition { get; set; }
        public TExpr Body { get; set; }

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
        public Position Position { get; private set; }

        public WhileExpr(Position position, IUnboundExpr condition, IUnboundExpr body) : base(condition, body)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Condition = Condition.AcceptTransformer(transformer);
            Body = Body.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }
    }

    /// <summary>
    /// A bound "while/do" expression.
    /// </summary>
    public class BoundWhileExpr : WhileExpr<IBoundExpr>, IBoundExpr
    {
        public BoundWhileExpr(IBoundExpr condition, IBoundExpr body) : base(condition, body) { }

        public IBoundDecl Type { get { return Body.Type; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
