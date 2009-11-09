using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.App
{
    public static class SelfHost
    {
        public static void Run()
        {
            //  compile the self-hosted compiler
            Script compiler = new Script(@"..\..\..\script\C2");
            compiler.Compile();

            // run it once with no arguments to run the unit tests
            compiler.Run(String.Empty);

            // now run it to compile a test script
            compiler.Run(@"..\..\..\script\Simple.mag");

            // get the compiled test script's bytecode
            var bytecode = compiler.Foreign.LastStream;

            if (bytecode == null)
            {
                Console.WriteLine("Error: compiler did not write bytecode.");
                return;
            }

            // run it
            Script testScript = new Script(bytecode);
            testScript.Run(String.Empty);
        }
    }
}
