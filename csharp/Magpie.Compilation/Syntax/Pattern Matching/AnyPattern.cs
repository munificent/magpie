using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Pattern matching expression that successfully matches any value.
    /// </summary>
    public class AnyPattern : Pattern, IPattern
    {
        public AnyPattern(Position position) : base(position) { }

        public override string ToString()
        {
            return "_";
        }

        #region ICaseExpr Members

        TReturn IPattern.Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
