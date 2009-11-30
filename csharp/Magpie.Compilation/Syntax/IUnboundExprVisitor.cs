using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IUnboundExprVisitor<TReturn>
    {
        TReturn Visit(UnitExpr expr);
        TReturn Visit(BoolExpr expr);
        TReturn Visit(IntExpr expr);
        TReturn Visit(StringExpr expr);

        TReturn Visit(NameExpr expr);

        TReturn Visit(ArrayExpr expr);
        TReturn Visit(TupleExpr expr);

        TReturn Visit(CallExpr expr);
        TReturn Visit(AssignExpr expr);
        TReturn Visit(BlockExpr expr);
        TReturn Visit(DefineExpr expr);
        TReturn Visit(FuncRefExpr expr);
        TReturn Visit(LocalFuncExpr expr);

        TReturn Visit(IfExpr expr);
        TReturn Visit(LetExpr expr);
        TReturn Visit(ReturnExpr expr);
        TReturn Visit(WhileExpr expr);
        TReturn Visit(LoopExpr expr);

        TReturn Visit(SyntaxExpr expr);

        /*
        TReturn Visit(MatchExpr expr);
         */
    }
}
