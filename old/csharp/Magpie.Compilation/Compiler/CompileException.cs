using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileException : Exception
    {
        public Position Position { get; private set; }

        //### bob: temp until we have positions for all compile errors
        public CompileException(string message)
            : this(Position.None, message)
        {
        }

        public CompileException(Position position, string message)
            : base(message)
        {
            Position = position;
        }
    }
}
