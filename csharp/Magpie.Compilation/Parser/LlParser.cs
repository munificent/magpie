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

            while (CurrentIs(separatorType))
            {
                Token separator = Consume(separatorType);

                IUnboundExpr right = parseFunc();

                left = combineFunc(left, separator, right);
            }

            return left;
        }

        protected T OneOrMoreRight<T>(Func<T> parseFunc, Func<T, T, T> combineFunc) where T : class
        {
            Stack<T> exprs = new Stack<T>();

            T expr = parseFunc();
            if (expr == null) throw new ParseException(Current.Position, "Parse error finding first token in a right-associative sequence. Found " + Current + " instead.");

            while (expr != null)
            {
                exprs.Push(expr);
                expr = parseFunc();
            }

            T result = null;

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

        private Token Current { get { return LookAhead(1); } }

        private Token Next { get { return LookAhead(2); } }

        protected bool CurrentIs(TokenType type)
        {
            return Current.Type == type;
        }

        protected bool CurrentIs(TokenType current, TokenType next)
        {
            return (Current.Type == current) && (Next.Type == next);
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

        protected bool ConsumeIf(TokenType type, out Position position)
        {
            if (CurrentIs(type))
            {
                position = Consume().Position;
                return true;
            }

            position = Position.None;
            return false;
        }

        protected bool ConsumeIf(TokenType current, TokenType next)
        {
            if ((Current.Type == current) && (Next.Type == next))
            {
                Consume();
                Consume();
                return true;
            }

            return false;
        }

        protected Token Consume()
        {
            Token token = mRead.Dequeue();
            mConsumed.Enqueue(token);

            return token;
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

        private Token LookAhead(int distance)
        {
            // read in as many as needed
            while (mRead.Count < distance)
            {
                if (mTokens.MoveNext())
                {
                    mRead.Enqueue(mTokens.Current);
                }
                else
                {
                    //### bob: hackish. position is fake.
                    mRead.Enqueue(new Token(Position.None, TokenType.Eof));
                }
            }

            // get the queued token
            return mRead.ElementAt(distance - 1);
        }

        private readonly IEnumerator<Token> mTokens;
        private readonly Queue<Token> mRead = new Queue<Token>();
        private readonly Queue<Token> mConsumed = new Queue<Token>(); // for debugging
    }
}
