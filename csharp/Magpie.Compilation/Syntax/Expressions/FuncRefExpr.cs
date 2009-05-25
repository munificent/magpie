using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class FuncRefExpr : IUnboundExpr
    {
        public Position Position { get; private set; }
        public NameExpr Name { get; private set; }
        public IUnboundDecl ParamType { get; private set; }

        public FuncRefExpr(Position position, NameExpr name, IUnboundDecl paramType)
        {
            Position = position;
            Name = name;
            ParamType = paramType;
        }

        public override string ToString()
        {
            return Name.ToString();
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
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
