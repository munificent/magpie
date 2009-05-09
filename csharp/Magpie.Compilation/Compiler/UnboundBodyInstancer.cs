using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class UnboundBodyInstancer : IUnboundExprVisitor<IUnboundExpr>
    {
        public static IUnboundExpr Instance(TypeArgApplicator applicator, IUnboundExpr body)
        {
            sInstance.mApplicator = applicator;

            return body.Accept(sInstance);
        }

        #region IUnboundExprVisitor<IUnboundExpr> Members

        public IUnboundExpr Visit(CallExpr expr)
        {
            return new CallExpr(expr.Target.Accept(this), expr.Arg.Accept(this));
        }

        public IUnboundExpr Visit(ArrayExpr expr)
        {
            return new ArrayExpr(expr.Elements.Accept(this), expr.IsMutable);
        }

        public IUnboundExpr Visit(AssignExpr expr)
        {
            return new AssignExpr(expr.Target.Accept(this), expr.Value.Accept(this));
        }

        public IUnboundExpr Visit(BlockExpr expr)
        {
            return new BlockExpr(expr.Exprs.Accept(this));
        }

        public IUnboundExpr Visit(DefineExpr expr)
        {
            return new DefineExpr(expr.Name, expr.Value.Accept(this), expr.IsMutable);
        }

        public IUnboundExpr Visit(FuncRefExpr expr)
        {
            throw new NotImplementedException();
        }

        public IUnboundExpr Visit(IfDoExpr expr)
        {
            return new IfDoExpr(
                expr.Condition.Accept(this),
                expr.Body.Accept(this));
        }

        public IUnboundExpr Visit(IfThenExpr expr)
        {
            return new IfThenExpr(
                expr.Condition.Accept(this),
                expr.ThenBody.Accept(this),
                expr.ElseBody.Accept(this));
        }

        public IUnboundExpr Visit(NameExpr expr)
        {
            return new NameExpr(expr.Name, mApplicator.ApplyTypes(expr.TypeArgs));
        }

        public IUnboundExpr Visit(OperatorExpr expr)
        {
            return new OperatorExpr(
                expr.Left.Accept(this),
                expr.Name,
                expr.Right.Accept(this));
        }

        public IUnboundExpr Visit(TupleExpr expr)
        {
            return new TupleExpr(expr.Fields.Accept(this));
        }

        public IUnboundExpr Visit(IntExpr expr) { return expr; }
        public IUnboundExpr Visit(BoolExpr expr) { return expr; }
        public IUnboundExpr Visit(StringExpr expr) { return expr; }
        public IUnboundExpr Visit(UnitExpr expr) { return expr; }

        public IUnboundExpr Visit(WhileExpr expr)
        {
            return new WhileExpr(
                expr.Condition.Accept(this),
                expr.Body.Accept(this));
        }

        public IUnboundExpr Visit(WrapBoundExpr expr)
        {
            // already bound, which implies it isn't affected by instantiation
            return expr;
            //return new WrapBoundExpr(BoundBodyInstancer.Instance(mApplicator, expr.Bound));
        }

        #endregion

        private UnboundBodyInstancer() { }

        private static UnboundBodyInstancer sInstance = new UnboundBodyInstancer();
        private TypeArgApplicator mApplicator;
    }
}
