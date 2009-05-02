using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IBoundExprVisitor<TReturn>
    {
        TReturn Visit(BoolExpr expr);
        TReturn Visit(UnitExpr expr);
        TReturn Visit(IntExpr expr);
        TReturn Visit(StringExpr expr);
        TReturn Visit(BoundArrayExpr expr);
        TReturn Visit(BoundTupleExpr expr);
        TReturn Visit(IntrinsicExpr expr);
        TReturn Visit(BoundCallExpr expr);
        TReturn Visit(BoundBlockExpr expr);
        TReturn Visit(BoundIfDoExpr expr);
        TReturn Visit(BoundIfThenExpr expr);
        TReturn Visit(BoundWhileExpr expr);
        TReturn Visit(LoadExpr expr);
        TReturn Visit(StoreExpr expr);
        TReturn Visit(LocalsExpr expr);
        TReturn Visit(BoundFuncRefExpr expr);
        TReturn Visit(ForeignCallExpr expr);
        TReturn Visit(ConstructExpr expr);
        TReturn Visit(ConstructUnionExpr expr);
    }
}
