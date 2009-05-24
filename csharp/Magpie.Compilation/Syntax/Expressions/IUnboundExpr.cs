using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Defines a unbound expression. This is what comes directly from parsing the source file, before
    /// identifiers have been bound, types have been checked, etc. An IUnboundExpr may or may not
    /// repesent valid code.
    /// </summary>
    public interface IUnboundExpr
    {
        Position Position { get; }
        TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor);
    }
}
