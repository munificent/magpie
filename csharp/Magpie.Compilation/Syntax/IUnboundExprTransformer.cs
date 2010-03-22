using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A transformer is a combination of a visitor and a hierarchical visitor. It will visit
    /// each expression and its subexpressions, calling the appropriate Transform() method for
    /// each expression. The transformed expression will be swapped with the value returned by
    /// the call to Transform(). This can be used to selectively replace or modify some
    /// expressions in an entire expression tree.
    /// </summary>
    public interface IUnboundExprTransformer
    {
        IUnboundExpr Transform(UnitExpr expr);
        IUnboundExpr Transform(BoolExpr expr);
        IUnboundExpr Transform(IntExpr expr);
        IUnboundExpr Transform(StringExpr expr);

        IUnboundExpr Transform(NameExpr expr);

        IUnboundExpr Transform(ArrayExpr expr);
        IUnboundExpr Transform(TupleExpr expr);

        IUnboundExpr Transform(CallExpr expr);
        IUnboundExpr Transform(AssignExpr expr);
        IUnboundExpr Transform(BlockExpr expr);
        IUnboundExpr Transform(DefineExpr expr);
        IUnboundExpr Transform(FuncRefExpr expr);
        IUnboundExpr Transform(LocalFuncExpr expr);

        IUnboundExpr Transform(IfExpr expr);
        IUnboundExpr Transform(LetExpr expr);
        IUnboundExpr Transform(ReturnExpr expr);
        IUnboundExpr Transform(WhileExpr expr);
        IUnboundExpr Transform(LoopExpr expr);

        IUnboundExpr Transform(MatchExpr expr);
        IUnboundExpr Transform(SyntaxExpr expr);
    }

    /// <summary>
    /// Base transformer implementation that makes no changes to the expressions.
    /// </summary>
    public abstract class UnboundExprTransformer : IUnboundExprTransformer
    {
        public virtual IUnboundExpr Transform(UnitExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(BoolExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(IntExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(StringExpr expr) { return expr; }

        public virtual IUnboundExpr Transform(NameExpr expr) { return expr; }

        public virtual IUnboundExpr Transform(ArrayExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(TupleExpr expr) { return expr; }

        public virtual IUnboundExpr Transform(CallExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(AssignExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(BlockExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(DefineExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(FuncRefExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(LocalFuncExpr expr) { return expr; }

        public virtual IUnboundExpr Transform(IfExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(LetExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(ReturnExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(WhileExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(LoopExpr expr) { return expr; }

        public virtual IUnboundExpr Transform(MatchExpr expr) { return expr; }
        public virtual IUnboundExpr Transform(SyntaxExpr expr) { return expr; }
    }
}
