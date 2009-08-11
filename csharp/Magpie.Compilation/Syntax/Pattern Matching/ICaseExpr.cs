using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface ICaseExpr
    {
        Position Position { get; }
        TReturn Accept<TReturn>(ICaseExprVisitor<TReturn> visitor);
    }
}
