using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.Foreign
{
    /// <summary>
    /// Contains the foreign functions available for the .NET interpreter.
    /// </summary>
    public class DotNetForeign : ForeignBase
    {
        public DotNetForeign()
        {
            // create the functions
            Add("System:Console:Clear", Decl.Unit, Clear);
            Add("System:Console:KeyAvailable?", Decl.Bool, KeyAvailable);
            Add("System:Console:ReadLine", Decl.String, ReadLine);
            Add("System:Console:Write", Decl.String, Decl.Unit, Write);
            Add("System:Console:WriteLine", Decl.String, Decl.Unit, WriteLine);
        }

        #region Foreign Functions

        Value Clear(Value[] args)
        {
            Console.Clear();
            return null;
        }

        Value KeyAvailable(Value[] args)
        {
            return new Value(Console.KeyAvailable);
        }

        Value ReadLine(Value[] args)
        {
            string read = Console.ReadLine();
            return new Value(read);
        }

        Value Write(Value[] args)
        {
            Console.Write(args[0]);
            return null;
        }

        Value WriteLine(Value[] args)
        {
            Console.WriteLine(args[0]);
            return null;
        }

        #endregion
    }
}
