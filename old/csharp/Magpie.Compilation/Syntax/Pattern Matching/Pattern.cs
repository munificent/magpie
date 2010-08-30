using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Base class for pattern matching expressions.
    /// </summary>
    public abstract class Pattern
    {
        public Position Position { get; private set; }

        protected Pattern(Position position)
        {
            Position = position;
        }
    }
}
