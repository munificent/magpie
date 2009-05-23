using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    //### bob: this should go away when we have pattern matching
    public class UnionValueGetter : ICallable
    {
        public UnionValueGetter(Union union, UnionCase unionCase)
        {
            mUnion = union;
            mCase = unionCase;
        }

        #region ICallable Members

        //### bob: need to qualify name
        public string Name { get { return mCase.Name + "Value"; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            return new LoadExpr(arg, mCase.ValueType.Bound, 1);
        }

        public IBoundDecl[] ParameterTypes
        {
            get { return new IBoundDecl[] { mUnion }; }
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
