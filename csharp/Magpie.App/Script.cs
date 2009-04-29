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
        public static void Run(string path, Action<string> printCallback)
        {
            sPrintCallback = printCallback;

            var foreign = new DotNetForeign();

            var compiler = new Compiler(foreign);
            compiler.AddSourceFile(path);

            using (var stream = new MemoryStream())
            {
                compiler.Compile(stream);

                stream.Seek(0, SeekOrigin.Begin);

                var machine = new Machine(foreign);
                machine.Printed += Machine_Printed;

                machine.Interpret(stream);
            }
        }

        static void Machine_Printed(object sender, PrintEventArgs e)
        {
            sPrintCallback(e.Text);
        }

        private static Action<string> sPrintCallback;
    }
}
