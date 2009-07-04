using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    /// <summary>
    /// Base class for runtime exceptions thrown by the interpreter.
    /// </summary>
    public class InterpreterException : Exception
    {
        public InterpreterException(string message) : base(message) { }
    }
}
