using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class NameCase : ICaseExpr
    {
        public string Name { get; private set; }

        public NameCase(string name)
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
