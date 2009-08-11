using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LiteralCase : ICaseExpr
    {
        public IUnboundExpr Value { get; private set; }

        //### bob: hackish. assumes value is a ValueExpr, which implements both
        // IUnboundExpr and IBoundExpr
        public IBoundDecl Type { get { return ((IBoundExpr)Value).Type; } }

        public LiteralCase(IUnboundExpr value)
        {
            Value = value;
        }

        public override string ToString()
        {
            return Value.ToString();
        }

        #region ICaseExpr Members

        public Position Position { get { return Value.Position; } }

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
