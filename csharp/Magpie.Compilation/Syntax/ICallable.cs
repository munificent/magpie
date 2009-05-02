using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Interface for any bound expression type that acts like a function:
    /// i.e. it can have an argument applied to it.
    /// </summary>
    public interface ICallable : IBoundExpr
    {
        void BindArgument(IBoundExpr arg);
    }
}
