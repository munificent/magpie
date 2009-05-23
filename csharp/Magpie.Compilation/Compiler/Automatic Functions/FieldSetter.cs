using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FieldSetter : ICallable
    {
        public FieldSetter(Struct structure, Field field)
        {
            mStruct = structure;
            mField = field;
        }

        #region ICallable Members

        //### bob: need to qualify name
        public string Name { get { return mField.Name + "<-"; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            var argTuple = (BoundTupleExpr)arg;

            return new StoreExpr(argTuple.Fields[0], mField, argTuple.Fields[1]);
        }

        public IBoundDecl[] ParameterTypes
        {
            get { return new IBoundDecl[] { mStruct, mField.Type.Bound }; }
        }

        public IBoundDecl[] TypeArguments { get { return mStruct.TypeArguments; } }

        public bool HasInferrableTypeArguments
        {
            get
            {
                // if a struct has type arguments, then they can be inferred for the field getter
                return mStruct.TypeArguments.Length > 0;
            }
        }

        #endregion

        private Struct mStruct;
        private Field mField;
    }
}
