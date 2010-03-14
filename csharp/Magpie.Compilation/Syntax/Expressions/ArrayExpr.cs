using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ArrayExpr : IUnboundExpr
    {
        public readonly List<IUnboundExpr> Elements = new List<IUnboundExpr>();
        public IUnboundDecl ElementType { get { return mElementType; } }
        public bool IsMutable { get; private set; }

        public Position Position { get; private set; }

        public ArrayExpr(Position position, IEnumerable<IUnboundExpr> elements, bool isMutable)
        {
            Position = position;
            Elements.AddRange(elements);
            IsMutable = isMutable;
        }

        public ArrayExpr(Position position, IUnboundDecl elementType, bool isMutable)
        {
            Position = position;
            mElementType = elementType;
            IsMutable = isMutable;
        }

        public override string ToString()
        {
            if (Elements.Count == 0)
            {
                return "[]'" + mElementType.ToString();
            }
            else
            {
                return "[" + Elements.JoinAll(", ") + "]";
            }
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            for (int i = 0; i < Elements.Count; i++)
            {
                Elements[i] = Elements[i].AcceptTransformer(transformer);
            }

            return transformer.Transform(this);
        }

        private IUnboundDecl mElementType; // only set if array is empty
    }
}