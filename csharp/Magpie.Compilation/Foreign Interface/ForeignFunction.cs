using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Identifies a foreign function.
    /// </summary>
    public class ForeignFunction
    {
        /// <summary>
        /// The ID for the function. This will be provided to the interpreter at runtime
        /// to select an appropriate foreign function to call.
        /// </summary>
        public int ID { get; private set; }

        /// <summary>
        /// The type declaration for the function.
        /// </summary>
        public FuncType Type { get; private set; }

        public ForeignFunction(string name, int id, FuncType type)
        {
            Type = type;
            ID = id;

            mUniqueName = BoundFunction.GetUniqueName(name, null, type.ParameterTypes);
        }

        public ForeignFunction(string name, int id, Decl returnType)
            : this(name, id, FuncType.Create(returnType)) {}

        public ForeignFunction(string name, int id, Decl arg, Decl returnType)
            : this(name, id, FuncType.Create(arg, returnType)) { }

        public ForeignFunction(string name, int id, Decl arg1, Decl arg2, Decl returnType)
            : this(name, id, FuncType.Create(arg1, arg2, returnType)) { }

        public ForeignFunction(string name, int id, Decl arg1, Decl arg2, Decl arg3, Decl returnType)
            : this(name, id, FuncType.Create(arg1, arg2, arg3, returnType)) { }

        /// <summary>
        /// Gets whether or not this function matches the given uniquely named function call.
        /// </summary>
        /// <param name="uniqueName">The unique name of the called function.</param>
        /// <returns><c>true</c> if this function matches that name.</returns>
        public bool Matches(string uniqueName)
        {
            return mUniqueName == uniqueName;
        }

        private string mUniqueName;
    }
}
