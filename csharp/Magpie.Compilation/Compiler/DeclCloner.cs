using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Creates deep clones of unbound type declarations.
    /// </summary>
    public class DeclCloner : IUnboundDeclVisitor<IUnboundDecl>
    {
        public static IUnboundDecl Clone(IUnboundDecl decl)
        {
            return decl.Accept(sCloner);
        }

        private DeclCloner() { }

        #region IUnboundDeclVisitor<IUnboundDecl> Members

        IUnboundDecl IUnboundDeclVisitor<IUnboundDecl>.Visit(AtomicDecl decl)
        {
            // atomic types are immutable, so we can reuse it
            return decl;
        }

        IUnboundDecl IUnboundDeclVisitor<IUnboundDecl>.Visit(FuncType decl)
        {
            return new FuncType(decl.Position,
                decl.Parameter.Unbound.Accept(this),
                decl.Return.Unbound.Accept(this));
        }

        IUnboundDecl IUnboundDeclVisitor<IUnboundDecl>.Visit(TupleType decl)
        {
            return new TupleType(decl.Fields.Accept(this));
        }

        IUnboundDecl IUnboundDeclVisitor<IUnboundDecl>.Visit(NamedType decl)
        {
            // named types are immutable, so we can reuse it
            return decl;
        }

        #endregion

        private static DeclCloner sCloner = new DeclCloner();
    }
}
