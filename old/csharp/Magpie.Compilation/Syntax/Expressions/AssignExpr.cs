using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// An assignment expression like "foo &lt;- bar"
    /// </summary>
    public class AssignExpr : IUnboundExpr
    {
        /// <summary>
        /// Gets the position of this expression in its source file.
        /// </summary>
        public Position Position { get; private set; }

        /// <summary>
        /// Gets the target lvalue of the expression: this what the assignment will assign into.
        /// </summary>
        public IUnboundExpr Target { get; private set; }

        /// <summary>
        /// Gets the expression for the value that will be assigned in the expression.
        /// </summary>
        public IUnboundExpr Value { get; private set; }

        /// <summary>
        /// Instantiates a new instance of AssignExpr.
        /// </summary>
        /// <param name="position">Where the expression occurs in the source file.</param>
        /// <param name="target">The target of the assignment.</param>
        /// <param name="value">The value being assigned.</param>
        public AssignExpr(Position position, IUnboundExpr target, IUnboundExpr value)
        {
            Position = position;
            Target = target;
            Value = value;
        }

        /// <summary>
        /// Overridden from Object.
        /// </summary>
        public override string ToString()
        {
            return String.Format("{0} <- {1}", Target, Value);
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Target = Target.AcceptTransformer(transformer);
            Value = Value.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }
    }
}
