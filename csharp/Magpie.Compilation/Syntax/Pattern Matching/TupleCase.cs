using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TupleCase : ICaseExpr
    {
        public IList<ICaseExpr> Fields { get; private set; }

        public TupleCase(IList<ICaseExpr> fields)
        {
            Fields = fields;
        }

        public override string ToString()
        {
            return "(" + Fields.JoinAll(", ") + ")";
        }

        #region ICaseExpr Members

        public Position Position
        {
            get
            {
                return Fields[0].Position;
            }
        }

        TReturn ICaseExpr.Accept<TReturn>(ICaseExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
