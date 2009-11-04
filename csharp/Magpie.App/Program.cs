using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.App
{
    static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main(string[] args)
        {
            if (args.Length != 1)
            {
                Console.WriteLine();
                Console.WriteLine("               _/Oo>");
                Console.WriteLine("              /(MM) ");
                Console.WriteLine("             A___/       m a g p i e");
                Console.WriteLine("     _______AV_h_h___________________________");
                Console.WriteLine("           AV");
                Console.WriteLine("          AV");
                Console.WriteLine("         AV");
                Console.WriteLine();
                Console.WriteLine("Usage: magpie <script path>");
                Console.WriteLine("       compiles and runs the script at that path");
                Console.WriteLine();
                Console.WriteLine("       magpie test");
                Console.WriteLine("       compiles and runs all of the test scripts");
                return;
            }

            if (args[0] == "test")
            {
                RunTests();
            }
            else
            {
                RunScript(args[0]);
            }

            // don't immediately quit (and close the console) if we're running attached
            // to the debugger
            if (System.Diagnostics.Debugger.IsAttached)
            {
                Console.WriteLine();
                Console.Write("Press any key to quit...");
                Console.ReadKey();
            }
        }

        private static void RunTests()
        {
            //### bob: hack. assumes location of tests relative to working directory. :(
            string testDir = Path.GetDirectoryName(Environment.CurrentDirectory);
            testDir = Path.GetDirectoryName(testDir);
            testDir = Path.GetDirectoryName(testDir);
            testDir = Path.Combine(testDir, "test");

            var suite = new TestSuite(testDir);
            suite.Run();
        }

        private static void RunScript(string script)
        {
            if (!File.Exists(script) && !Directory.Exists(script))
            {
                Console.WriteLine("Could not find a script at \"" + script + "\".");
                return;
            }

            IList<CompileError> errors = Script.Run(script, text => Console.WriteLine(text));

            // show the errors if any
            foreach (var error in errors)
            {
                Console.WriteLine("{0} error in {1} (line {2}, col {3}): {4}",
                    error.Stage, error.Position.File, error.Position.Line,
                    error.Position.Column, error.Message);
            }
        }

        static void Machine_Printed(object sender, PrintEventArgs e)
        {
            Console.WriteLine(e.Text);
        }
    }
}
