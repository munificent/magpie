using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ArrayExpr<TExpr, TDecl> where TDecl : class
    {
        public readonly List<TExpr> Elements = new List<TExpr>();
        public TDecl ElementType { get { return mElementType; } }
        public bool IsMutable { get; private set; }

        public ArrayExpr(IEnumerable<TExpr> elements, bool isMutable)
            : this(null, elements, isMutable)
        {
        }

        /// <summary>
        /// Used for empty arrays.
        /// </summary>
        /// <param name="elementType"></param>
        public ArrayExpr(TDecl elementType, bool isMutable)
            : this(elementType, null, isMutable)
        {
        }

        protected ArrayExpr(TDecl elementType, IEnumerable<TExpr> elements, bool isMutable)
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

        private TDecl mElementType; // only set if array is empty
    }

    public class ArrayExpr : ArrayExpr<IUnboundExpr, IUnboundDecl>, IUnboundExpr
    {
        public Position Position { get; private set; }

        public ArrayExpr(Position position, IEnumerable<IUnboundExpr> elements, bool isMutable)
            : base(elements, isMutable)
        {
            Position = position;
        }

        public ArrayExpr(Position position, IUnboundDecl elementType, bool isMutable)
            : base(elementType, isMutable)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoundArrayExpr : ArrayExpr<IBoundExpr, IBoundDecl>, IBoundExpr
    {
        public BoundArrayExpr(IBoundDecl elementType, IEnumerable<IBoundExpr> elements, bool isMutable)
            : base(elementType, elements, isMutable)
        { }

        public IBoundDecl Type
        {
            get { return new BoundArrayType(ElementType, IsMutable); }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
