using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class VariablePattern : Pattern, IPattern
    {
        /// <summary>
        /// The name of the variable.
        /// </summary>
        public string Name { get; private set; }

        public VariablePattern(Position position, string name)
            : base(position)
        {
            Name = name;
        }

        public override string ToString()
        {
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
