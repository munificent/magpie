using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IUnboundDeclVisitor<TReturn>
    {
        TReturn Visit(AtomicDecl decl);
        TReturn Visit(FuncType decl);
        TReturn Visit(TupleType decl);
        TReturn Visit(NamedType decl);
    }

    public interface IBoundDeclVisitor<TReturn>
    {
        TReturn Visit(BoundArrayType decl);
        TReturn Visit(AtomicDecl decl);
        TReturn Visit(FuncType decl);
        TReturn Visit(Struct decl);
        TReturn Visit(BoundTupleType decl);
        TReturn Visit(Union decl);
        TReturn Visit(ForeignType decl);
    }
}
