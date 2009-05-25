using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IGenericCallable
    {
        string Name { get; }

        ICallable Instantiate(Compiler compiler, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType);
    }
}
