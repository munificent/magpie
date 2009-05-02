using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: to get this fully working, i'll need to get the real binary format for
    //  bytecode files working. right now, we use function "indexes" in a couple of 
    //  places and moving to a dictionary from a flat list makes those undefined.
    //  once the bytecode file is using full offsets, the indexes can go away.
    //  i will probably need to make a jump table though for the bytecode generator
    //  to track where the generated functions are.
    public class SymbolTable
    {
        public IEnumerable<BoundFunction> Values { get { return mSymbols.Values; } }

        public void Add(BoundFunction function)
        {
            string uniqueName = GetUniqueName(function.Name, function.Type.ParameterTypes);

            if (mSymbols.ContainsKey(uniqueName)) throw new CompileException("There is already a bound function named " + uniqueName + ".");

            mSymbols.Add(uniqueName, function);
        }

        public bool TryFind(string name, IEnumerable<Decl> paramTypes, out BoundFunction bound)
        {
            string uniqueName = GetUniqueName(name, paramTypes);

            return mSymbols.TryGetValue(uniqueName, out bound);
        }

        //### bob: is this the best place for this?
        private string GetUniqueName(string name, IEnumerable<Decl> paramTypes)
        {
            return name + "_(" + paramTypes.JoinAll(", ") + ")";
        }

        private readonly Dictionary<string, BoundFunction> mSymbols = new Dictionary<string, BoundFunction>();
    }
}
