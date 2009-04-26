using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.App
{
    public static class Script
    {
        public static void Run(string path, Action<string> printCallback)
        {
            sPrintCallback = printCallback;

            var compiler = new Compiler();
            compiler.AddSourceFile(path);

            using (var stream = new MemoryStream())
            {
                compiler.Compile(stream);

                stream.Seek(0, SeekOrigin.Begin);

                var machine = new Machine();
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
