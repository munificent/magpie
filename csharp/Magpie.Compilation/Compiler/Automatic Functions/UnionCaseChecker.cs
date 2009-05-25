using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class UnionCaseChecker : ICallable
    {
        public UnionCaseChecker(Union union, UnionCase unionCase)
        {
            mUnion = union;
            mCase = unionCase;
        }

        #region ICallable Members

        //### bob: need to qualify name
        public string Name { get { return mCase.Name + "?"; } }

        public IBoundExpr CreateCall(IBoundExpr arg)
        {
            //### opt: unions with no value don't need to be references
            //         could just put the value in place
            var loadCase = new LoadExpr(arg, Decl.Int, 0);

            return Intrinsic.EqualInt(loadCase, new IntExpr(mCase.Index));
        }

        public IBoundDecl ParameterType
        {
            get { return mUnion; }
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
