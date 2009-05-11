using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileException : Exception
    {
        public TokenPosition Position { get; private set; }

        //### bob: temp until we have positions for all compile errors
        public CompileException(string message)
            : this(TokenPosition.None, message)
        {
        }

        public CompileException(TokenPosition position, string message)
            : base(message)
        {
            Position = position;
        }
    }
}
