using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FieldGetter : ICallable
    {
        public FieldGetter(Struct structure, Field field)
        {
            mStruct = structure;
            mField = field;
        }

        #region ICallable Members

        //### bob: need to qualify name
        public string Name { get { return mField.Name; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new LoadExpr(arg, mField);
        }

        public IBoundDecl ParameterType
        {
            get { return mStruct; }
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
