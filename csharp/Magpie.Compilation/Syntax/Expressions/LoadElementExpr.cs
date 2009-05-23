using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Loads an element from an array.
    /// </summary>
    /// <remarks>If we really wanted to be minimal about the number of opcodes, this
    /// could also be used in place of LoadExpr, but that would require pushing every
    /// field index onto the stack.</remarks>
    public class LoadElementExpr : IBoundExpr
    {
        public IBoundExpr Array { get; private set; }
        public IBoundExpr Index { get; private set; }

        public LoadElementExpr(IBoundExpr structure, IBoundExpr index)
        {
            Array = structure;
            Index = index;
        }

        public IBoundDecl Type
        {
            get
            {
                var arrayType = (BoundArrayType)Array.Type;
                return arrayType.ElementType;
            }
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
