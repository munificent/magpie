using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class BoundFunction : ICallable
    {
        /// <summary>
        /// A unique name for the function, including type arguments and value argument types.
        /// </summary>
        public string Name { get { return mName; } }

        public FuncType Type { get { return Unbound.FuncType; } }

        public IBoundExpr Body { get { return mBody; } }

        public int NumLocals { get { return mNumLocals; } }

        public BoundFunction(string name)
        {
            mName = name;
        }

        public void Bind(IBoundExpr body, int numLocals)
        {
            mBody = body;
            mNumLocals = numLocals;
        }

        #region ICallable Members

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new BoundCallExpr(new BoundFuncRefExpr(this), arg);
        }

        #endregion

        private readonly string mName;
        public Function Unbound;
        private IBoundExpr mBody;
        private int mNumLocals;
    }
}
