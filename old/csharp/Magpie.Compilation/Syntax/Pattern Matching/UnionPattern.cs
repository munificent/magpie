using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Pattern that matches against a single named case in a union.
    /// </summary>
    public class UnionPattern : Pattern, IPattern
    {
        /// <summary>
        /// The name of the union case.
        /// </summary>
        public string Name { get; private set; }

        /// <summary>
        /// Pattern to match the union's value against, if the union has a
        /// value. Will be <c>null</c> if not used.
        /// </summary>
        public IPattern Value { get; private set; }

        public UnionPattern(Position position, string name, IPattern value)
            : base(position)
        {
            Name = name;
            Value = value;
        }

        public override string ToString()
        {
            if (Value != null) return String.Format("{0} {1}", Name, Value);

            return Name.ToString();
        }

        #region ICaseExpr Members

        TReturn IPattern.Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
