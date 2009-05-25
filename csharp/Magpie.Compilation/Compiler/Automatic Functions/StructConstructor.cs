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

        public IBoundDecl ParameterType
        {
            get
            {
                if (mStruct.Fields.Count == 0) return Decl.Unit;
                if (mStruct.Fields.Count == 1) return mStruct.Fields[0].Type.Bound;

                return new BoundTupleType(mStruct.Fields.Select(field => field.Type.Bound));
            }
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
