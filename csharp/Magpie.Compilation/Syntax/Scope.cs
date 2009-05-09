using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A local variable lexical scope.
    /// </summary>
    public class Scope
    {
        /// <summary>
        /// Gets the number of variable slots needed for this scope. This is the
        /// maximum number of variables that have been defined at one time.
        /// </summary>
        public int NumVariables { get { return mNumVariables; } }

        public Field this[string name] { get { return mStruct[name]; } }

        /// <summary>
        /// Starts a new inner scope.
        /// </summary>
        public void Push()
        {
            // remember where this scope starts
            mInnerScopes.Push(mStruct.Fields.Count);
        }

        /// <summary>
        /// Closes this scope and discards locals defined in it.
        /// </summary>
        public void Pop()
        {
            // forget every local defined in the inner scope
            int start = mInnerScopes.Pop();
            if (start < mStruct.Fields.Count)
            {
                mStruct.Fields.RemoveRange(start, mStruct.Fields.Count - start);
            }
        }

        public bool Contains(string name)
        {
            return mStruct.Contains(name);
        }

        public void Define(string name, Decl type, bool isMutable)
        {
            mStruct.Define(name, type, isMutable);

            // track the highwater
            mNumVariables = Math.Max(mNumVariables, mStruct.Fields.Count);
        }

        private readonly Struct mStruct = new Struct("_scope", null, null);
        private readonly Stack<int> mInnerScopes = new Stack<int>();
        private int mNumVariables = 0;
    }
}
