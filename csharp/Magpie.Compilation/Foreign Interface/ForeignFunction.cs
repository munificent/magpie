using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Identifies a foreign function.
    /// </summary>
    public class ForeignFunction : ICallable
    {
        public string Name { get; private set; }

        /// <summary>
        /// The ID for the function. This will be provided to the interpreter at runtime
        /// to select an appropriate foreign function to call.
        /// </summary>
        public int ID { get; private set; }

        /// <summary>
        /// The type declaration for the function.
        /// </summary>
        public FuncType FuncType { get; private set; }

        public ForeignFunction(string name, int id, FuncType type)
        {
            Name = name;
            FuncType = type;
            ID = id;
        }

        public ForeignFunction(string name, int id, Decl returnType)
            : this(name, id, FuncType.Create(returnType)) {}

        public ForeignFunction(string name, int id, Decl arg, Decl returnType)
            : this(name, id, FuncType.Create(arg, returnType)) { }

        public ForeignFunction(string name, int id, Decl arg1, Decl arg2, Decl returnType)
            : this(name, id, FuncType.Create(arg1, arg2, returnType)) { }

        public ForeignFunction(string name, int id, Decl arg1, Decl arg2, Decl arg3, Decl returnType)
            : this(name, id, FuncType.Create(arg1, arg2, arg3, returnType)) { }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new ForeignCallExpr(this, arg);
        }

        #endregion
    }
}
