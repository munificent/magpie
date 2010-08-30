using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Convenience class for programmatically generating AST.
    /// </summary>
    public class CodeBuilder
    {
        public CodeBuilder(NameGenerator generator)
            : this(generator, Position.None)
        {
        }

        public CodeBuilder(NameGenerator generator, Position position)
        {
            mGenerator = generator;
            mPosition = position;
        }

        public void SetPosition(Position position)
        {
            mPosition = position;
        }

        public string TempName()
        {
            return mGenerator.Generate();
        }

        public IUnboundExpr Assign(IUnboundExpr target, IUnboundExpr value)
        {
            return new AssignExpr(mPosition, target, value);
        }

        public IUnboundExpr Block(IEnumerable<IUnboundExpr> exprs)
        {
            return new BlockExpr(exprs);
        }

        public IUnboundExpr Block(params IUnboundExpr[] exprs)
        {
            return new BlockExpr(exprs);
        }

        public IUnboundExpr Call(string name, string arg)
        {
            return Call(name, new NameExpr(mPosition, arg));
        }

        public IUnboundExpr Call(string name, IUnboundExpr arg)
        {
            return new CallExpr(new NameExpr(mPosition, name), arg);
        }

        public IUnboundExpr Call(IUnboundExpr target, IUnboundExpr arg)
        {
            return new CallExpr(target, arg);
        }

        public IUnboundExpr Def(string name, IUnboundExpr value)
        {
            return new DefineExpr(mPosition, name, value, false);
        }

        public IUnboundExpr Def(IList<string> names, IUnboundExpr value)
        {
            return new DefineExpr(mPosition, names, value, false);
        }

        public IUnboundExpr If(IUnboundExpr condition, IUnboundExpr thenBody)
        {
            return new IfExpr(mPosition, condition, thenBody, null);
        }

        public IUnboundExpr If(IUnboundExpr condition, IUnboundExpr thenBody, IUnboundExpr elseBody)
        {
            return new IfExpr(mPosition, condition, thenBody, elseBody);
        }

        public IUnboundExpr Int(int value)
        {
            return new IntExpr(mPosition, value);
        }

        public IUnboundExpr Op(IUnboundExpr left, string op, IUnboundExpr right)
        {
            return Call(op, Tuple(left, right));
        }

        public IUnboundExpr Tuple(params IUnboundExpr[] fields)
        {
            return new TupleExpr(fields);
        }

        public IUnboundExpr While(IUnboundExpr condition, IUnboundExpr body)
        {
            return new WhileExpr(mPosition, condition, body);
        }

        private NameGenerator mGenerator;
        private Position mPosition;
    }
}
