using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IUnboundExprVisitor<TReturn>
    {
        TReturn Visit(CallExpr expr);
        TReturn Visit(ArrayExpr expr);
        TReturn Visit(AssignExpr expr);
        TReturn Visit(BlockExpr expr);
        TReturn Visit(DefineExpr expr);
        TReturn Visit(FuncRefExpr expr);
        TReturn Visit(IfThenExpr expr);
        TReturn Visit(IfThenElseExpr expr);
        TReturn Visit(NameExpr expr);
        TReturn Visit(OperatorExpr expr);
        TReturn Visit(TupleExpr expr);
        TReturn Visit(IntExpr expr);
        TReturn Visit(BoolExpr expr);
        TReturn Visit(StringExpr expr);
        TReturn Visit(ReturnExpr expr);
        TReturn Visit(UnitExpr expr);
        TReturn Visit(WhileExpr expr);
        TReturn Visit(ForExpr expr);
        TReturn Visit(WrapBoundExpr expr);
    }
}
