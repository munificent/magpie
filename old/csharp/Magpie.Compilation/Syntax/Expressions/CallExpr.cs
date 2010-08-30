using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Base class for a bound or unbound function call.
    /// </summary>
    /// <typeparam name="TExpr"></typeparam>
    public class CallExpr<TExpr>
    {
        public TExpr Target;
        public TExpr Arg;

        public CallExpr(TExpr target, TExpr arg)
        {
            Target = target;
            Arg = arg;
        }

        public override string ToString() { return String.Format("{0} {1}", Target, Arg); }
    }

    /// <summary>
    /// Unbound function call.
    /// </summary>
    public class CallExpr : CallExpr<IUnboundExpr>, IUnboundExpr
    {
        public Position Position { get { return Target.Position; } }

        public CallExpr(IUnboundExpr target, IUnboundExpr arg)
            : base(target, arg)
        {
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Target = Target.AcceptTransformer(transformer);
            Arg = Arg.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        public override string ToString() { return String.Format("{0} {1}", Target, Arg); }
    }

    /// <summary>
    /// Bound function call. This is used for both prefix and infix functions, but
    /// not for intrinsics or foreign function calls.
    /// </summary>
    public class BoundCallExpr : CallExpr<IBoundExpr>, IBoundExpr
    {
        public BoundCallExpr(IBoundExpr target, IBoundExpr arg) : base(target, arg) { }

        public IBoundDecl Type
        {
            get { return ((FuncType)Target.Type).Return.Bound; }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
