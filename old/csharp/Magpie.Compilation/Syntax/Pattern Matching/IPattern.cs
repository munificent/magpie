using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A pattern matching expression.
    /// </summary>
    public interface IPattern
    {
        Position Position { get; }
        TReturn Accept<TReturn>(IPatternVisitor<TReturn> visitor);
    }
}
