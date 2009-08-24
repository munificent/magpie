using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Pattern match expression to match against a union case, and its
    /// value if appropriate.
    /// </summary>
    public class UnionCaseCase : CaseBase, ICaseExpr
    {
        public string Name { get; private set; }

        public ICaseExpr Value { get; private set; }

        public UnionCaseCase(Position position, string name, ICaseExpr value)
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

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
