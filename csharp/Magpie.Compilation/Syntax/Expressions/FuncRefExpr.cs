using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FuncRefExpr : IUnboundExpr
    {
        public Position Position { get; private set; }

        public NameExpr Name { get { return mName; } }
        public IUnboundDecl[] ParamTypes { get { return mParamTypes.ToArray(); } }

        public FuncRefExpr(Position position, NameExpr name, IEnumerable<IUnboundDecl> paramTypes)
        {
            Position = position;
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
        private readonly List<IUnboundDecl> mParamTypes = new List<IUnboundDecl>();
    }

    public class BoundFuncRefExpr : IBoundExpr
    {
        public Function Function { get; private set; }

        public IBoundDecl Type { get { return Function.Type; } }

        public BoundFuncRefExpr(Function function)
        {
            Function = function;
        }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
