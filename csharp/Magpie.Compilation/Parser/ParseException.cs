using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class ParseException : Exception
    {
        public ParseException(TokenPosition position, string message)
            : base(String.Format("Parse error at ({0},{1}:{2}): {3}", position.Line, position.Column, position.Length, message))
        { }
    }
}
