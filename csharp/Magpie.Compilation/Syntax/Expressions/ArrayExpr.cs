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
        public bool IsMutable { get; private set; }

        public ArrayExpr(IEnumerable<TExpr> elements, bool isMutable)
            : this(null, elements, isMutable)
        {
        }

        /// <summary>
        /// Used for empty arrays.
        /// </summary>
        /// <param name="elementType"></param>
        public ArrayExpr(Decl elementType, bool isMutable)
            : this(elementType, null, isMutable)
        {
        }

        protected ArrayExpr(Decl elementType, IEnumerable<TExpr> elements, bool isMutable)
        {
            mElementType = elementType;
            IsMutable = isMutable;

            if (elements != null) Elements.AddRange(elements);
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
        public ArrayExpr(IEnumerable<IUnboundExpr> elements, bool isMutable) : base(elements, isMutable) { }
        public ArrayExpr(Decl elementType, bool isMutable) : base(elementType, isMutable) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundArrayExpr : ArrayExpr<IBoundExpr>, IBoundExpr
    {
        public BoundArrayExpr(Decl elementType, IEnumerable<IBoundExpr> elements, bool isMutable)
            : base(elementType, elements, isMutable)
        { }

        public Decl Type
        {
            get { return new ArrayType(ElementType, IsMutable); }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
