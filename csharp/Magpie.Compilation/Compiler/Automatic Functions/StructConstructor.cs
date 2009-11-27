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
            if (mStruct.Fields.Count > 1)
            {
                // the struct has multiple fields, the arg tuple *is* the struct
                // so we just return it with the right type
                return new BoundTupleExpr(((BoundTupleExpr)arg).Fields,
                    mStruct);
            }
            else if (mStruct.Fields.Count == 1)
            {
                // the struct has only one field, the arg will just be a value.
                // in that case, we need to hoist it into a tuple to make it
                // properly a reference type.
                //### opt: this is really only needed for mutable single-field
                //    structs. for immutable ones, pass by reference and pass by value
                //    are indistinguishable.
                return new BoundTupleExpr(new IBoundExpr[] { arg }, mStruct);
            }
            else
            {
                // for an empty struct, just include a dummy value
                //### bob: this is gross
                return new BoundTupleExpr(new IBoundExpr[] { new IntExpr(0) }, mStruct);
            }
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
