using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Userd for type declarations for which there is only a single instance: int, bool, etc.
    /// </summary>
    public class AtomicDecl : Decl
    {
        public AtomicDecl(string name)
        {
            mName = name;
        }

        public override string ToString()
        {
            return mName;
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private string mName;
    }
}
