using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileException : Exception
    {
        public CompileException(string message)
            : base(message)
        { }
    }
}
