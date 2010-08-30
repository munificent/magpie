using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IGenericCallable
    {
        string Name { get; }

        ICallable Instantiate(BindingContext context, IEnumerable<IBoundDecl> typeArgs,
            IBoundDecl argType);
    }
}
