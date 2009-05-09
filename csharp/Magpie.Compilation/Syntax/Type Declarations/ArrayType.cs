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
        public Decl ElementType { get; private set; }
        public bool IsMutable { get; private set; }

        public ArrayType(Decl elementType, bool isMutable)
        {
            ElementType = elementType;
            IsMutable = isMutable;
        }

        public override string ToString()
        {
            return "Array'" + ElementType.ToString();
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
