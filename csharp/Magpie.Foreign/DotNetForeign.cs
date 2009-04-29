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
    public class DotNetForeign : IForeignStaticInterface, IForeignRuntimeInterface
    {
        public DotNetForeign()
        {
            //### bob: when the ffi supports namespaces, these should be namespaced

            // create the functions
            Add("Clear", Decl.Unit, Clear);
            Add("KeyAvailable?", Decl.Bool, KeyAvailable);
            Add("ReadLine", Decl.String, ReadLine);
            Add("Write", Decl.String, Decl.Unit, Write);
            Add("WriteLine", Decl.String, Decl.Unit, WriteLine);
        }

        private void Add(string name, Decl returnType, Func<Value[], Value> func)
        {
            int id = mFunctions.Count;

            var foreignFunction = new ForeignFunction(name, id, returnType);
            mFunctions[id] = new KeyValuePair<ForeignFunction, Func<Value[], Value>>(foreignFunction, func);
        }

        private void Add(string name, Decl arg, Decl returnType, Func<Value[], Value> func)
        {
            int id = mFunctions.Count;

            var foreignFunction = new ForeignFunction(name, id, arg, returnType);
            mFunctions[id] = new KeyValuePair<ForeignFunction, Func<Value[], Value>>(foreignFunction, func);
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

        #region IForeignInterface Members

        public ForeignFunction FindFunction(string uniqueName)
        {
            foreach (var foreign in mFunctions.Values)
            {
                if (foreign.Key.Matches(uniqueName)) return foreign.Key;
            }

            // no foreign calls matched
            return null;
        }

        #endregion

        #region IForeignRuntimeInterface Members

        public Value ForeignCall(int id, Value[] args)
        {
            var func = mFunctions[id].Value;
            return func(args);
        }

        #endregion

        private readonly Dictionary<int, KeyValuePair<ForeignFunction, Func<Value[], Value>>> mFunctions = new Dictionary<int, KeyValuePair<ForeignFunction, Func<Value[], Value>>>();
    }
}
