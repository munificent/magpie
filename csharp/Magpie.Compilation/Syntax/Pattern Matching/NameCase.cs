using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class NameCase : CaseBase, ICaseExpr
    {
        public string Name { get; private set; }

        public NameCase(Position position, string name)
            : base(position)
        {
            Name = name;
        }

        public override string ToString()
        {
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
