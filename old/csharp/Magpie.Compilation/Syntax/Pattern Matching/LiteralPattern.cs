using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Base pattern class for matching an explicit literal value of an atomic type.
    /// </summary>
    public abstract class LiteralPattern
    {
        public Position Position { get { return Token.Position; } }

        public abstract IBoundDecl Type { get; }
        public abstract IUnboundExpr ValueExpr { get; }

        public LiteralPattern(Token token)
        {
            Token = token;
        }

        public override string ToString()
        {
            return Token.ToString();
        }

        protected Token Token { get; private set; }
    }

    /// <summary>
    /// A boolean literal pattern.
    /// </summary>
    public class BoolPattern : LiteralPattern, IPattern
    {
        public bool Value { get { return Token.BoolValue; } }

        public override IBoundDecl Type { get { return Decl.Bool; } }
        public override IUnboundExpr ValueExpr { get { return new BoolExpr(Token); } }

        public BoolPattern(Token token) : base(token) { }

        public TReturn Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    /// <summary>
    /// An int literal pattern.
    /// </summary>
    public class IntPattern : LiteralPattern, IPattern
    {
        public int Value { get { return Token.IntValue; } }

        public override IBoundDecl Type { get { return Decl.Int; } }
        public override IUnboundExpr ValueExpr { get { return new IntExpr(Token); } }

        public IntPattern(Token token) : base(token) { }

        public TReturn Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }

    /// <summary>
    /// A string literal pattern.
    /// </summary>
    public class StringPattern : LiteralPattern, IPattern
    {
        public string Value { get { return Token.StringValue; } }

        public override IBoundDecl Type { get { return Decl.String; } }
        public override IUnboundExpr ValueExpr { get { return new StringExpr(Token); } }

        public StringPattern(Token token) : base(token) { }

        public TReturn Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
