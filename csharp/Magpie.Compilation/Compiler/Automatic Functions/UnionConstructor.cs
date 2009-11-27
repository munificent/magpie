using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class UnionConstructor : ICallable
    {
        public UnionConstructor(Union union, UnionCase unionCase)
        {
            mUnion = union;
            mCase = unionCase;
        }

        #region ICallable Members

        //### bob: need to qualify name
        public string Name { get { return mCase.Name; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            var fields = new List<IBoundExpr>();

            // add the case tag
            fields.Add(new IntExpr(mCase.Index));

            // add the value, if any
            if (!(arg is UnitExpr))
            {
                fields.Add(arg);
            }

            // create the structure
            return new BoundTupleExpr(fields, mCase.Union);
        }

        public IBoundDecl ParameterType
        {
            get { return mCase.ValueType.Bound; }
        }

        public IBoundDecl[] TypeArguments { get { return mUnion.TypeArguments; } }

        public bool HasInferrableTypeArguments
        {
            get
            {
                return mCase.HasInferrableTypeArguments;
            }
        }

        #endregion

        private Union mUnion;
        private UnionCase mCase;
    }
}
