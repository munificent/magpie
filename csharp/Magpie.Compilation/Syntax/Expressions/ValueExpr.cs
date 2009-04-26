using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public abstract class ValueExpr<TValue>
    {
        public TValue Value;

        protected ValueExpr(TValue value)
        {
            Value = value;
        }
    }

    public class IntExpr : ValueExpr<int>, IUnboundExpr, IBoundExpr
    {
        public IntExpr(int value) : base(value) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return Value.ToString(); }

        public Decl Type { get { return Decl.Int; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class BoolExpr : ValueExpr<bool>, IUnboundExpr, IBoundExpr
    {
        public BoolExpr(bool value) : base(value) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return Value.ToString(); }

        public Decl Type { get { return Decl.Bool; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    public class StringExpr : ValueExpr<string>, IUnboundExpr
    {
        public StringExpr(string value) : base(value) { }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public override string ToString() { return "\"" + Value + "\""; }
    }

    public class BoundStringExpr : IBoundExpr
    {
        public int Index;

        public BoundStringExpr(int index)
        {
            Index = index;
        }

        public override string ToString() { return "[string at " + Index + "]"; }

        public Decl Type { get { return Decl.String; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
