using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public abstract class ValueExpr<TValue>
    {
        public Position Position { get; private set; }
        public TValue Value;

        protected ValueExpr(Position position, TValue value)
        {
            Position = position;
            Value = value;
        }
    }

    public class IntExpr : ValueExpr<int>, IUnboundExpr, IBoundExpr
    {
        public IntExpr(Position position, int value) : base(position, value) { }
        public IntExpr(int value) : base(Position.None, value) { }
        public IntExpr(Token token) : base(token.Position, token.IntValue) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return Value.ToString(); }

        public IBoundDecl Type { get { return Decl.Int; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoolExpr : ValueExpr<bool>, IUnboundExpr, IBoundExpr
    {
        public BoolExpr(Position position, bool value) : base(position, value) { }
        public BoolExpr(Token token) : base(token.Position, token.BoolValue) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return Value.ToString(); }

        public IBoundDecl Type { get { return Decl.Bool; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class StringExpr : ValueExpr<string>, IUnboundExpr, IBoundExpr
    {
        public StringExpr(Position position, string value) : base(position, value) { }
        public StringExpr(Token token) : base(token.Position, token.StringValue) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IBoundDecl Type { get { return Decl.String; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return "\"" + Value + "\""; }
    }
}
