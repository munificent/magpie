using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Scope : Struct
    {
        public Scope Parent;

        public Scope(Scope parent) : base("_scope", null, null)
        {
            Parent = parent;
        }

        public Scope() : this(null) { }
    }
}
