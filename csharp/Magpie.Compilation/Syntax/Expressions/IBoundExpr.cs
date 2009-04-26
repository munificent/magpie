using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Represents a bound expression. This is created by binding an unbound expression, and represents
    /// a fully compilable valid expression.
    /// </summary>
    public interface IBoundExpr
    {
        Decl Type { get; }
        TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor);
    }
}
