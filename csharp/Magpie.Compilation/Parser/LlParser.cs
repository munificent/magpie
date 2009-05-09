using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class LlParser
    {
        public LlParser(IEnumerable<Token> tokens)
        {
            mTokens = tokens.GetEnumerator();
        }

        protected IUnboundExpr OneOrMoreLeft(TokenType separatorType, Func<IUnboundExpr> parseFunc, Func<IUnboundExpr, Token, IUnboundExpr, IUnboundExpr> combineFunc)
        {
            IUnboundExpr left = parseFunc();
            //if (left == null) throw new ParseException(Current.Position, "Parse error finding first token in a left-associative sequence. Found " + Current + " instead.");

            while (CurrentIs(separatorType))
            {
                Token separator = Consume(separatorType);

                IUnboundExpr right = parseFunc();

                left = combineFunc(left, separator, right);
            }

            return left;
        }

        protected IUnboundExpr OneOrMoreRight(Func<IUnboundExpr> parseFunc, Func<IUnboundExpr, IUnboundExpr, IUnboundExpr> combineFunc)
        {
            Stack<IUnboundExpr> exprs = new Stack<IUnboundExpr>();

            IUnboundExpr expr = parseFunc();
            if (expr == null) throw new ParseException(Current.Position, "Parse error finding first token in a right-associative sequence. Found " + Current + " instead.");

            while (expr != null)
            {
                exprs.Push(expr);
                expr = parseFunc();
            }

            IUnboundExpr result = null;

            while (exprs.Count > 0)
            {
                if (result == null)
                {
                    result = exprs.Pop();
                }
                else
                {
                    result = combineFunc(exprs.Pop(), result);
                }
            }

            return result;
        }

        public Token Current
        {
            get
            {
                if (mRead.Count == 0)
                {
                    if (mTokens.MoveNext())
                    {
                        mRead.Enqueue(mTokens.Current);
                    }
                    else
                    {
                        //### bob: hackish. position is fake.
                        mRead.Enqueue(new Token(new TokenPosition(0, 0, 0), TokenType.Eof));
                    }
                }

                return mRead.Peek();
            }
        }

        protected bool CurrentIs(TokenType type)
        {
            return Current.Type == type;
        }

        protected bool ConsumeIf(TokenType type)
        {
            if (CurrentIs(type))
            {
                Consume();
                return true;
            }

            return false;
        }

        protected void Consume()
        {
            mConsumed.Enqueue(mRead.Dequeue());
        }

        protected Token Consume(TokenType type)
        {
            if (Current.Type == type)
            {
                Token token = Current;
                Consume();
                return token;
            }

            throw new ParseException(Current.Position, "Found token " + Current.Type + " when looking for " + type + ".");
        }

        private readonly IEnumerator<Token> mTokens;
        private readonly Queue<Token> mRead = new Queue<Token>();
        private readonly Queue<Token> mConsumed = new Queue<Token>(); // for debugging
    }
}
