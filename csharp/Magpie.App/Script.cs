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
            return Run(path, 0, printCallback);
        }

        public static IList<CompileError> Run(string path, int maxStackDepth, Action<string> printCallback)
        {
            sPrintCallback = printCallback;

            var foreign = new DotNetForeign();

            var compiler = new Compiler(foreign);

            //### bob: temp. work-in-progress debug stuff
            DebugInfo debug = new DebugInfo();
            compiler.FunctionStarted = debug.StartFunction;

            // add the base sources
            //### bob: hack. assumes location of base relative to working directory. :(
            string baseDir = Path.GetDirectoryName(Environment.CurrentDirectory);
            baseDir = Path.GetDirectoryName(baseDir);
            baseDir = Path.GetDirectoryName(baseDir);
            baseDir = Path.Combine(baseDir, "base");

            foreach (var baseFile in Directory.GetFiles(baseDir, "*.mag"))
            {
                compiler.AddSourceFile(baseFile);
            }

            // add the main file(s) to compile
            if (Directory.Exists(path))
            {
                // path is a directory, so include all files in it
                foreach (var file in Directory.GetFiles(path, "*.mag", SearchOption.AllDirectories))
                {
                    compiler.AddSourceFile(file);
                }
            }
            else
            {
                // path is a single file
                compiler.AddSourceFile(path);
            }

            using (var stream = new MemoryStream())
            {
                IList<CompileError> errors = compiler.Compile(stream);

                // bail if there were compile errors
                if (errors.Count > 0) return errors;

                stream.Seek(0, SeekOrigin.Begin);

                var machine = new Machine(foreign);
                machine.Printed += Machine_Printed;
                machine.MaxStackDepth = maxStackDepth;

                try
                {
                    machine.Interpret(stream, debug, "argument!");
                }
                catch (InterpreterException ex)
                {
                    // do nothing
                    //### bob: should report runtime errors
                    Console.WriteLine(ex.ToString());
                }

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
