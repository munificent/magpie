using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Foreign;
using Magpie.Interpreter;

namespace Magpie.App
{
    public static class Script
    {
        public static IList<CompileError> Run(string path, Action<string> printCallback)
        {
            sPrintCallback = printCallback;

            var foreign = new DotNetForeign();

            var compiler = new Compiler(foreign);
            compiler.AddSourceFile(path);

            using (var stream = new MemoryStream())
            {
                IList<CompileError> errors = compiler.Compile(stream);

                // bail if there were compile errors
                if (errors.Count > 0) return errors;

                stream.Seek(0, SeekOrigin.Begin);

                var machine = new Machine(foreign);
                machine.Printed += Machine_Printed;

                machine.Interpret(stream);

                return errors;
            }
        }

        static void Machine_Printed(object sender, PrintEventArgs e)
        {
            sPrintCallback(e.Text);
        }

        private static Action<string> sPrintCallback;
    }
}
