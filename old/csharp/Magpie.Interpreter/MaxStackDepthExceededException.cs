using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Interpreter
{
    public class MaxStackDepthExceededException : InterpreterException
    {
        public MaxStackDepthExceededException(int limit)
            : base(String.Format("The interpreter has exceeded its maximum stack depth of {0}.", limit))
        { }
    }
}
