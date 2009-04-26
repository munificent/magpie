using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ArrayExpr<TExpr>
    {
        public readonly List<TExpr> Elements = new List<TExpr>();
        public Decl ElementType { get { return mElementType; } }

        public ArrayExpr(IEnumerable<TExpr> elements)
        {
            Elements.AddRange(elements);
        }

        /// <summary>
        /// Used for empty arrays.
        /// </summary>
        /// <param name="elementType"></param>
        public ArrayExpr(Decl elementType)
        {
            mElementType = elementType;
        }

        protected ArrayExpr(Decl elementType, IEnumerable<TExpr> elements)
        {
            mElementType = elementType;
            Elements.AddRange(elements);
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

        private Decl mElementType; // only set if array is empty
    }

    public class ArrayExpr : ArrayExpr<IUnboundExpr>, IUnboundExpr
    {
        public ArrayExpr(IEnumerable<IUnboundExpr> elements) : base(elements) { }
        public ArrayExpr(Decl elementType) : base(elementType) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundArrayExpr : ArrayExpr<IBoundExpr>, IBoundExpr
    {
        public BoundArrayExpr(Decl elementType, IEnumerable<IBoundExpr> elements)
            : base(elementType, elements)
        { }

        public Decl Type
        {
            get  { return new ArrayType(ElementType); }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
