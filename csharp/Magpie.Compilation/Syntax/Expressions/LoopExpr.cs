using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LoopExpr : IUnboundExpr
    {
        public IList<LoopClause> Clauses { get; private set; }
        public IUnboundExpr Body { get; private set; }

        public LoopExpr(Position position, IList<LoopClause> clauses, IUnboundExpr body)
        {
            Position = position;
            Clauses = clauses;
            Body = body;
        }

        #region IUnboundExpr Members

        public Position Position { get; private set; }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    /// <summary>
    /// Represents a single clause in a loop expression.
    /// </summary>
    public class LoopClause
    {
        public Position Position { get; private set; }

        public bool IsWhile { get { return String.IsNullOrEmpty(Name); } }

        /// <summary>
        /// Gets the name of the value created by a for loop. Will be empty if
        /// the clause is a while loop.
        /// </summary>
        public string Name { get; private set; }

        /// <summary>
        /// Gets the expression for the clause. If it is a while clause, this
        /// is the condition. For a for clause, this is the generator expression.
        /// </summary>
        public IUnboundExpr Expression { get; private set; }

        public LoopClause(Position position, string name, IUnboundExpr expression)
        {
            Position = position;
            Name = name;
            Expression = expression;
        }
    }
}
