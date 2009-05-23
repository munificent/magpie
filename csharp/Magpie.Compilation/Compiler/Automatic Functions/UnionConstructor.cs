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
            return new ConstructUnionExpr(mCase, arg);
        }

        public IBoundDecl[] ParameterTypes
        {
            get { return mCase.ValueType.Bound.Expand(); }
        }

        public IBoundDecl[] TypeArguments { get { return mUnion.TypeArguments; } }

        public bool HasInferrableTypeArguments
        {
            get
            {
                // if a union has type arguments, then they can be inferred
                return mUnion.TypeArguments.Length > 0;
            }
        }

        #endregion

        private Union mUnion;
        private UnionCase mCase;
    }
}
