using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Special wildcard type that matches any type. Used internally for
    /// intrinsics such as array Size that need to match arrays of any type.
    /// </summary>
    public class AnyType : Decl
    {
        public override string ToString()
        {
            return "*";
        }

        public override TReturn Accept<TReturn>(IDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
