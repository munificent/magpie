using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface ICallable
    {
        string Name { get; }
        IBoundExpr CreateCall(IBoundExpr arg);
        IBoundDecl[] ParameterTypes { get; }

        bool HasInferrableTypeArguments { get; }
        IBoundDecl[] TypeArguments { get; }
    }
}
