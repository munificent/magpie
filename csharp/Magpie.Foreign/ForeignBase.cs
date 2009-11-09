using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.Foreign
{
    public abstract class ForeignBase : IForeignStaticInterface, IForeignRuntimeInterface
    {
        protected ForeignBase()
        {
        }

        protected void Add(string name, IBoundDecl returnType, Func<Value[], Value> func)
        {
            int id = mFunctions.Count;

            var foreignFunction = new ForeignFunction(name, id, returnType);
            mFunctions[id] = new KeyValuePair<ForeignFunction, Func<Value[], Value>>(foreignFunction, func);
        }

        protected void Add(string name, IBoundDecl arg, IBoundDecl returnType, Func<Value[], Value> func)
        {
            int id = mFunctions.Count;

            var foreignFunction = new ForeignFunction(name, id, arg, returnType);
            mFunctions[id] = new KeyValuePair<ForeignFunction, Func<Value[], Value>>(foreignFunction, func);
        }

        #region IForeignStaticInterface Members

        public IEnumerable<ForeignFunction> Functions
        {
            get { return mFunctions.Values.Select(pair => pair.Key); }
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
