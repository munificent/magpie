using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LocalFuncExpr : IUnboundExpr
    {
        public Position Position { get; private set; }
        public readonly List<string> ParamNames = new List<string>();
        public FuncType Type { get; private set; }
        public IUnboundExpr Body { get; private set; }

        public LocalFuncExpr(Position position, IEnumerable<string> paramNames, FuncType type, IUnboundExpr body)
        {
            Position = position;
            ParamNames.AddRange(paramNames);
            Type = type;
            Body = body;
        }

        #region IUnboundExpr Members

        TReturn IUnboundExpr.Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            Body = Body.AcceptTransformer(transformer);

            return transformer.Transform(this);
        }

        #endregion
    }
}
