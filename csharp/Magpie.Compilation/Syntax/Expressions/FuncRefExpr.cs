using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FuncRefExpr : IUnboundExpr
    {
        public NameExpr Name { get { return mName; } }
        public Decl[] ParamTypes { get { return mParamTypes.ToArray(); } }

        public FuncRefExpr(NameExpr name, IEnumerable<Decl> paramTypes)
        {
            mName = name;
            mParamTypes.AddRange(paramTypes);
        }

        public override string ToString()
        {
            return mName.ToString();
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private NameExpr mName;
        private readonly List<Decl> mParamTypes = new List<Decl>();
    }

    public class BoundFuncRefExpr : IBoundExpr
    {
        public BoundFunction Function { get { return mFunction; } }

        public Decl Type
        {
            get { return mFunction.Type; }
        }

        public BoundFuncRefExpr(BoundFunction function)
        {
            mFunction = function;
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        private BoundFunction mFunction;
    }
}
