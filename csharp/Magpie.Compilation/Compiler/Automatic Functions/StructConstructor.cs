using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class StructConstructor : ICallable
    {
        public StructConstructor(Struct structure)
        {
            mStruct = structure;
        }

        #region ICallable Members

        public string Name { get { return mStruct.Name; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new ConstructExpr(mStruct, arg);
        }

        public IBoundDecl[] ParameterTypes
        {
            get { return mStruct.Fields.Select(field => field.Type.Bound).ToArray(); }
        }

        public IBoundDecl[] TypeArguments { get { return mStruct.TypeArguments; } }

        public bool HasInferrableTypeArguments
        {
            get
            {
                // if a struct has type arguments, then they can be inferred for the constructor
                return mStruct.TypeArguments.Length > 0;
            }
        }

        #endregion

        private Struct mStruct;
    }
}
