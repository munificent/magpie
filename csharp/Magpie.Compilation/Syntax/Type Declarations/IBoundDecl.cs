using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IBoundDecl
    {
        TReturn Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor);
    }

    public static class BoundDeclExtensions
    {
        public static IBoundDecl[] Expand(this IBoundDecl decl)
        {
            // the unit type expands to no values
            if (ReferenceEquals(decl, Decl.Unit)) return new IBoundDecl[0];

            // a tuple expands to its fields
            BoundTupleType tuple = decl as BoundTupleType;
            if (tuple != null)
            {
                return tuple.Fields.ToArray();
            }

            // everything else expands to just itself
            return new IBoundDecl[] { decl };
        }
    }
}
