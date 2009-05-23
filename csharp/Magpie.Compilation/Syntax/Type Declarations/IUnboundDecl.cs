using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IUnboundDecl
    {
        TokenPosition Position { get; }
        TReturn Accept<TReturn>(IUnboundDeclVisitor<TReturn> visitor);
    }

    public static class UnboundDeclExtensions
    {
        public static IUnboundDecl[] Expand(this IUnboundDecl decl)
        {
            // the unit type expands to no values
            if (ReferenceEquals(decl, Decl.Unit)) return new IUnboundDecl[0];

            // a tuple expands to its fields
            TupleType tuple = decl as TupleType;
            if (tuple != null)
            {
                return tuple.Fields.ToArray();
            }

            // everything else expands to just itself
            return new IUnboundDecl[] { decl };
        }
    }

}
