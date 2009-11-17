using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class IfExpr<TExpr>
    {
        public TExpr Condition;
        public TExpr ThenBody;

        /// <summary>
        /// Gets the expression for the else arm. Will be <c>null</c> if there
        /// is no else arm.
        /// </summary>
        public TExpr ElseBody;

        public IfExpr(TExpr condition, TExpr thenBody, TExpr elseBody)
        {
            Condition = condition;
            ThenBody = thenBody;
            ElseBody = elseBody;
        }

        public override string ToString() { return String.Format("if {0} then {1} else {2}", Condition, ThenBody, ElseBody); }
    }

    public class IfExpr : IfExpr<IUnboundExpr>, IUnboundExpr
    {
        public Position Position { get; private set; }

        public IfExpr(Position position, IUnboundExpr condition, IUnboundExpr thenBody, IUnboundExpr elseBody)
        : base(condition, thenBody, elseBody)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundIfExpr : IfExpr<IBoundExpr>, IBoundExpr
    {
        public BoundIfExpr(IBoundExpr condition, IBoundExpr thenBody, IBoundExpr elseBody) : base(condition, thenBody, elseBody) { }

        public IBoundDecl Type
        {
            get
            {
                // if there is no "else" branch, the expression type is always unit
                // (even if the "then" branch's type is actually EarlyReturn)
                if (ElseBody == null) return Decl.Unit;

                return ((IBoundExpr)ThenBody).Type;
            }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
