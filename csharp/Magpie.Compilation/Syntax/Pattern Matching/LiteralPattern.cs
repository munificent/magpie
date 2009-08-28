using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Pattern for matching an explicit literal value of an atomic type.
    /// </summary>
    /*
    public class LiteralPattern : IPattern
    {
        /// <summary>
        /// Gets the value this pattern will successfully match.
        /// </summary>
        public IUnboundExpr Value { get; private set; }

        //### bob: hackish. assumes value is a ValueExpr, which implements both
        // IUnboundExpr and IBoundExpr
        public IBoundDecl Type { get { return ((IBoundExpr)Value).Type; } }

        public LiteralPattern(IUnboundExpr value)
        {
            Value = value;
        }

        public override string ToString()
        {
            return Value.ToString();
        }

        #region ICaseExpr Members

        public Position Position { get { return Value.Position; } }

        TReturn IPattern.Accept<TReturn>(IPatternVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }
    */

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
