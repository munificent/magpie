using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileError
    {
        public CompileStage Stage { get; private set; }
        public Position Position { get; private set; }
        public string Message { get; private set; }

        public CompileError(CompileStage stage, Position position, string message)
        {
            Stage = stage;
            Position = position;
            Message = message;
        }
    }

    public enum CompileStage
    {
        Scan,
        Parse,
        Compile
    }
}
