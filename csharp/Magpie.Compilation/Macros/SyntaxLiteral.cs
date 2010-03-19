using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Given the expression inside a SyntaxExpr, converts it to a 
    /// corresponding expression that will construct the appropriate
    /// Magpie Expression object. For example, given:
    /// { 1 + 2 }
    /// This will compile "1 + 2" to "Call Tuple [Int 1, Int 2]"
    /// </summary>
    public class SyntaxLiteral : IUnboundExprVisitor<IUnboundExpr>
    {
        public static IUnboundExpr Desugar(IUnboundExpr expr)
        {
            var instance = new SyntaxLiteral();
            return expr.Accept(instance);
        }

        #region IUnboundExprVisitor<IUnboundExpr> Members

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(UnitExpr expr)
        {
            return Call("UnitExpr", Unit());
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(BoolExpr expr)
        {
            return Call("BoolExpr", expr);
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(IntExpr expr)
        {
            return Call("IntExpr", expr);
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(StringExpr expr)
        {
            return Call("StringExpr", expr);
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(NameExpr expr)
        {
            return Call("NameExpr", String(expr.Name));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(ArrayExpr expr)
        {
            return Call("ArrayExpr", Array(expr.Elements.Accept(this)));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(TupleExpr expr)
        {
            return Call("TupleExpr", Array(expr.Fields.Accept(this)));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(CallExpr expr)
        {
            return Call("CallExpr", Tuple(expr.Target.Accept(this), expr.Arg.Accept(this)));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(AssignExpr expr)
        {
            return Call("AssignExpr", Tuple(expr.Target.Accept(this), expr.Value.Accept(this)));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(BlockExpr expr)
        {
            return Call("BlockExpr", Array(expr.Exprs.Accept(this)));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(DefineExpr expr)
        {
            /*
            //### bob: hack temp. doesn't work with multiple defines
            return Call("DefineExpr", Tuple(
                Bool(expr.IsMutable),
                String(expr.Definitions[0].Name),
                expr.Definitions[0].Value.Accept(this)));
            */
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(FuncRefExpr expr)
        {
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(LocalFuncExpr expr)
        {
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(IfExpr expr)
        {
            return Call("IfExpr", Tuple(expr.Condition.Accept(this),
                                        expr.ThenBody.Accept(this),
                                        (expr.ElseBody != null) ? expr.ElseBody.Accept(this) : Call("UnitExpr", Unit())));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(LetExpr expr)
        {
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(ReturnExpr expr)
        {
            return Call("ReturnExpr", expr.Value.Accept(this));
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(WhileExpr expr)
        {
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(LoopExpr expr)
        {
            throw new NotImplementedException();
        }

        IUnboundExpr IUnboundExprVisitor<IUnboundExpr>.Visit(SyntaxExpr expr)
        {
            throw new NotImplementedException();
        }

        #endregion

        private SyntaxLiteral()
        {
        }

        private IUnboundExpr Call(IUnboundExpr target, IUnboundExpr arg)
        {
            return new CallExpr(target, arg);
        }

        private IUnboundExpr Call(string name, IUnboundExpr arg)
        {
            return Call(new NameExpr(Position.None, name), arg);
        }

        private IUnboundExpr Unit()
        {
            return new UnitExpr(Position.None);
        }

        private IUnboundExpr Bool(bool value)
        {
            return new BoolExpr(Position.None, value);
        }

        private IUnboundExpr String(string value)
        {
            return new StringExpr(Position.None, value);
        }

        private IUnboundExpr Tuple(params IUnboundExpr[] exprs)
        {
            return new TupleExpr(exprs);
        }

        private IUnboundExpr Array(IEnumerable<IUnboundExpr> exprs)
        {
            return new ArrayExpr(Position.None, exprs);
        }
    }
}
