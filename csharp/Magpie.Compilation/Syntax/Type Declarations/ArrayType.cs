using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines an array type declaration, including the type of the elements.
    /// </summary>
    public class ArrayType : Decl
    {
        public Decl ElementType { get { return mElementType; } }

        public ArrayType(Decl elementType)
        {
            mElementType = elementType;
        }

        public override string ToString()
        {
            return "Array'" + mElementType.ToString();
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private readonly Decl mElementType;
    }
}
