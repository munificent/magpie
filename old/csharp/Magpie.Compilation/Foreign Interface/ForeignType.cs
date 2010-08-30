using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Represents a named type generated and used by a foreign interface.
    /// To the rest of Magpie, the type is essentially an opaque object
    /// with a name.
    /// </summary>
    public class ForeignType : INamedType
    {
        public ForeignType(string name)
        {
            Name = name;
        }

        #region INamedType Members

        public Position Position { get { return Position.None; } }

        public string Name { get; private set; }

        public IBoundDecl[] TypeArguments
        {
            get { return new IBoundDecl[0]; }
        }

        #endregion

        #region IBoundDecl Members

        public TReturn Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
}
