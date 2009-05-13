using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class CompileError
    {
        public CompileStage Stage { get; private set; }
        public int Line { get; private set; }
        public string Message { get; private set; }

        public CompileError(CompileStage stage, int line, string message)
        {
            Stage = stage;
            Line = line;
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
