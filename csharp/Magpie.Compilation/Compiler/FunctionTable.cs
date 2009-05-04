using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Maintains the collection of compiled functions used by a program.
    /// </summary>
    public class FunctionTable
    {
        public IEnumerable<BoundFunction> Functions
        {
            get { return mSymbols.Values; }
        }

        /// <summary>
        /// Gets the declared function for the given unbound function.
        /// </summary>
        /// <param name="function"></param>
        /// <returns></returns>
        public BoundFunction Find(Function function)
        {
            var uniqueName = GetUniqueName(function.FullName, function.TypeParameters, function.FuncType.ParameterTypes);

            return mSymbols[uniqueName];
        }

        /// <summary>
        /// Declares a function. This adds the function within its namespace to the symbol table,
        /// but implies the function has not been compiled yet. Functions are declared before
        /// being compiled so that recursive and mutually recursive references can be looked up.
        /// </summary>
        public void Declare(Function function)
        {
            var uniqueName = GetUniqueName(function.FullName, function.TypeParameters, function.FuncType.ParameterTypes);

            if (mSymbols.ContainsKey(uniqueName)) throw new CompileException("A function named " + uniqueName + " has already been declared.");

            BoundFunction bound = new BoundFunction(uniqueName);
            bound.Unbound = function;

            mSymbols.Add(uniqueName, bound);
        }

        public bool TryFind(string name, IEnumerable<Decl> paramTypes, out BoundFunction bound)
        {
            string uniqueName = GetUniqueName(name, paramTypes);

            return mSymbols.TryGetValue(uniqueName, out bound);
        }

        public void BindAll(CompileUnit unit)
        {
            foreach (BoundFunction func in mSymbols.Values.ToArray())
            {
                FunctionBinder.Bind(unit, func, null);
            }
        }

        public static string GetUniqueName(string name, IEnumerable<Decl> typeArgs, IEnumerable<Decl> paramTypes)
        {
            string typeArgString = ((typeArgs != null) && typeArgs.Any()) ? "[" + typeArgs.JoinAll(", ") + "]" : "";
            string argTypes = "(" + paramTypes.JoinAll(", ") + ")";
            return name + "__" + typeArgString + argTypes;
        }

        //### bob: is this the best place for this?
        public static string GetUniqueName(string name, IEnumerable<Decl> paramTypes)
        {
            return GetUniqueName(name, null, paramTypes);
        }

        private readonly Dictionary<string, BoundFunction> mSymbols = new Dictionary<string, BoundFunction>();
    }
}
