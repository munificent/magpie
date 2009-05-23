using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Used for type declarations for which there is only a single instance: int, bool, etc.
    /// </summary>
    public class AtomicDecl : IUnboundDecl, INamedType
    {
        public TokenPosition Position { get { return TokenPosition.None; } }

        public string Name { get; private set; }

        public AtomicDecl(string name)
        {
            Name = name;
        }

        public override string ToString()
        {
            return Name;
        }

        #region IUnboundDecl Members

        TReturn IUnboundDecl.Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
