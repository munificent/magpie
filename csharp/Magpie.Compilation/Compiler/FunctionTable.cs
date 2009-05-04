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

        public IEnumerable<BoundFunction> Functions
        {
            get { return mCallables.Values.OfType<BoundFunction>(); }
        }

        /// <summary>
        /// Declares a function. This adds the function within its namespace to the symbol table,
        /// but implies the function has not been compiled yet. Functions are declared before
        /// being compiled so that recursive and mutually recursive references can be looked up.
        /// </summary>
        public BoundFunction Add(Function function)
        {
            BoundFunction bound = new BoundFunction();
            bound.Unbound = function;

            Add(bound, function.FullName, function.TypeParameters, function.FuncType.ParameterTypes);
            return bound;
        }

        public void Add(IEnumerable<Intrinsic> intrinsics)
        {
            foreach (var intrinsic in intrinsics)
            {
                Add(intrinsic, intrinsic.Name, null, intrinsic.FuncType.ParameterTypes);
            }
        }

        public void Add(IEnumerable<ForeignFunction> foreigns)
        {
            foreach (var foreign in foreigns)
            {
                Add(foreign, foreign.Name, null, foreign.FuncType.ParameterTypes);
            }
        }

        public bool TryFind(string name, IEnumerable<Decl> paramTypes, out ICallable bound)
        {
            string uniqueName = GetUniqueName(name, paramTypes);

            return mCallables.TryGetValue(uniqueName, out bound);
        }

        public void BindAll(CompileUnit unit)
        {
            // copy the functions to an array because binding a function may cause generics
            // to be instantiated, adding to the collection.
            // (we don't need to worry about binding the newly added generics, because they
            // are bound as part of the instantiation process. binding is how the compiler
            // determines if a generic's type arguments are valid.)
            foreach (BoundFunction func in Functions.ToArray())
            {
                FunctionBinder.Bind(unit, func, null);
            }
        }

        private void Add(ICallable callable, string name, IEnumerable<Decl> typeArgs, IEnumerable<Decl> paramTypes)
        {
            var uniqueName = GetUniqueName(name, typeArgs, paramTypes);

            //### bob: if we want users to be able to override intrinsics, we may need to handle this differently
            if (mCallables.ContainsKey(uniqueName)) throw new CompileException("A function named " + uniqueName + " has already been declared.");

            mCallables.Add(uniqueName, callable);
        }

        private readonly Dictionary<string, ICallable> mCallables = new Dictionary<string, ICallable>();
    }
}
