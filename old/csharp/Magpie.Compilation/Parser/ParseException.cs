using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ParseException : Exception
    {
        public ParseException(Position position, string message)
            : base(String.Format("Parse error at (line {0} column {1}-{2}): {3}", position.Line, position.Column, position.Column + position.Length, message))
        { }
    }
}
