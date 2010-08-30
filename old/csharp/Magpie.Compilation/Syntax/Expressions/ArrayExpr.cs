using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: this should go away. there are no longer array literals. instead there's an "ArrayOf" function.
    // once that is a proper intrinsic instead of the hacked stuff in FunctionBinder, this can go away.
    /// <summary>
    /// An array literal expression like [1, 2, 3].
    /// </summary>
    public class ArrayExpr : IUnboundExpr
    {
        /// <summary>
        /// Gets the collection of elements in the array. May be be empty.
        /// </summary>
        public readonly List<IUnboundExpr> Elements = new List<IUnboundExpr>();

        /// <summary>
        /// Gets the array element type. All elements of an array must be the same type.
        /// </summary>
        public IUnboundDecl ElementType { get { return mElementType; } }

        /// <summary>
        /// Gets the position of this expression in its source file.
        /// </summary>
        public Position Position { get; private set; }

        /// <summary>
        /// Instantiates a new instance of ArrayExpr.
        /// </summary>
        /// <param name="position">Where the expression occurs in the source file.</param>
        /// <param name="elements">The array elements.</param>
        public ArrayExpr(Position position, IEnumerable<IUnboundExpr> elements)
        {
            Position = position;
            Elements.AddRange(elements);
        }

        /// <summary>
        /// Instantiates a new instance of a zero-length array.
        /// </summary>
        /// <param name="position">Where the expression occurs in the source file.</param>
        /// <param name="elementType">The element type. This must be explicitly passed in since
        /// it cannot be inferred from an element.</param>
        public ArrayExpr(Position position, IUnboundDecl elementType)
        {
            Position = position;
            mElementType = elementType;
        }

        /// <summary>
        /// Overridden from Object.
        /// </summary>
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