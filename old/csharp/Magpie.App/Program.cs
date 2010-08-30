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
            if (args.Length == 0)
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
            else if (args[0] == "self")
            {
                SelfHost.Run();
            }
            else
            {
                RunScript(args[0], args.Length > 1 ? args[1] : String.Empty);
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

        private static void RunScript(string path, string argument)
        {
            if (!File.Exists(path) && !Directory.Exists(path))
            {
                Console.WriteLine("Could not find a script at \"" + path + "\".");
                return;
            }

            Script script = new Script(path);

            script.Run(argument);

            // show the errors if any
            foreach (var error in script.Errors)
            {
                Console.WriteLine("{0} error in {1} (line {2}, col {3}): {4}",
                    error.Stage, error.Position.File, error.Position.Line,
                    error.Position.Column, error.Message);
            }
        }
    }
}
