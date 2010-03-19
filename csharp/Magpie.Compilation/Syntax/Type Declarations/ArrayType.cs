using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines an array type declaration, including the type of the elements.
    /// </summary>
    public class ArrayType<TDecl>
    {
        public TDecl ElementType { get; private set; }

        public ArrayType(TDecl elementType)
        {
            ElementType = elementType;
        }

        public override string ToString()
        {
            return "[]'" + ElementType.ToString();
        }
    }
    
    /// <summary>
    /// Defines an array type declaration, including the type of the elements.
    /// </summary>
    public class ArrayType : ArrayType<IUnboundDecl>, IUnboundDecl
    {
        public Position Position { get; private set; }

        public ArrayType(Position position, IUnboundDecl elementType)
            : base(elementType)
        {
            Position = position;
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class BoundArrayType : ArrayType<IBoundDecl>, IBoundDecl
    {
        public BoundArrayType(IBoundDecl elementType)
            : base(elementType)
        {
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
