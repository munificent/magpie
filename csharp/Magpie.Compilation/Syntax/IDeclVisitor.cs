using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IDeclVisitor<TReturn>
    {
        TReturn Visit(AnyType decl);
        TReturn Visit(ArrayType decl);
        TReturn Visit(AtomicDecl decl);
        TReturn Visit(FuncType decl);
        TReturn Visit(NamedType decl);
        TReturn Visit(TupleType decl);
    }
}
