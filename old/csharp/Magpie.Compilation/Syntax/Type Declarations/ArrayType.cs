using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines an array type declaration, including the type of the elements.
    /// </summary>
    public class BoundArrayType : IBoundDecl
    {
        public IBoundDecl ElementType { get; private set; }

        public BoundArrayType(IBoundDecl elementType)
        {
            ElementType = elementType;
        }

        public override string ToString()
        {
            return "Array[" + ElementType.ToString() + "]";
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
