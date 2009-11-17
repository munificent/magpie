using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class Scanner : IEnumerable<Token>
    {
        public Scanner(string fileName, string source)
        {
            mFile = fileName;
            mSource = source;
        }

        public Token Scan()
        {
            Token token = null;

            while (token == null)
            {
                token = ScanCharacter();

                if (token != null)
                {
                    // look up keywords
                    if (token.Type == TokenType.Name)
                    {
                        switch (token.StringValue)
                        {
                            case "case": token = new Token(token.Position, TokenType.Case); break;
                            case "def": token = new Token(token.Position, TokenType.Def); break;
                            case "do": token = new Token(token.Position, TokenType.Do); break;
                            case "else": token = new Token(token.Position, TokenType.Else); break;
                            case "end": token = new Token(token.Position, TokenType.End); break;
                            case "fn": token = new Token(token.Position, TokenType.Fn); break;
                            case "for": token = new Token(token.Position, TokenType.For); break;
                            case "if": token = new Token(token.Position, TokenType.If); break;
                            case "let": token = new Token(token.Position, TokenType.Let); break;
                            case "match": token = new Token(token.Position, TokenType.Match); break;
                            case "mutable": token = new Token(token.Position, TokenType.Mutable); break;
                            case "var": token = new Token(token.Position, TokenType.Mutable); break;
                            case "namespace": token = new Token(token.Position, TokenType.Namespace); break;
                            case "return": token = new Token(token.Position, TokenType.Return); break;
                            case "struct": token = new Token(token.Position, TokenType.Struct); break;
                            case "then": token = new Token(token.Position, TokenType.Then); break;
                            case "union": token = new Token(token.Position, TokenType.Union); break;
                            case "using": token = new Token(token.Position, TokenType.Using); break;
                            case "while": token = new Token(token.Position, TokenType.While); break;
                            case "true": token = new Token(token.Position, true); break;
                            case "false": token = new Token(token.Position, false); break;
                        }
                    }

                    // look up operator keywords
                    if (token.Type == TokenType.Operator)
                    {
                        switch (token.StringValue)
                        {
                            case "<-": token = new Token(token.Position, TokenType.LeftArrow); break;
                            case "->": token = new Token(token.Position, TokenType.RightArrow); break;
                        }
                    }
                }
            }

            return token;
        }

        private enum ScanState
        {
            Default,
            InString,
            InStringEscape,
            InName,
            InOperator,
            InNumber,
            InMinus,
            InLineComment,
            InBlockComment
        }

        // Scans a single character. Returns a token if one is completed,
        // or null otherwise.
        private Token ScanCharacter()
        {
            switch (mState)
            {
                case ScanState.Default:
                    if (IsDone)                      return Eof;

                    // skip whitespace
                    else if (Match (" "))            return null;
                    else if (Match ("\r"))           return null;

                    else if (Match ("/", "/"))       return StartToken(ScanState.InLineComment);
                    else if (Match ("/", "*"))       return StartToken(ScanState.InBlockComment);

                    else if (Match ("("))            return new Token(LastChar, TokenType.LeftParen);
                    else if (Match (")"))            return new Token(LastChar, TokenType.RightParen);
                    else if (Match ("["))            return new Token(LastChar, TokenType.LeftBracket);
                    else if (Match ("]", "!"))       return new Token(LastChar, TokenType.RightBracketBang);
                    else if (Match ("]"))            return new Token(LastChar, TokenType.RightBracket);
                    else if (Match (","))            return new Token(LastChar, TokenType.Comma);
                    else if (Match (":"))            return new Token(LastChar, TokenType.Colon);
                    else if (Match ("."))            return new Token(LastChar, TokenType.Dot);
                    else if (Match ("\n"))           return new Token(LastChar, TokenType.Line);
                    else if (Match("'"))             return new Token(LastChar, TokenType.Prime);

                    else if (Current == "\"")        return StartToken(ScanState.InString);
                    else if (Current == "-")         return StartToken(ScanState.InMinus);
                    else if (IsAlpha(Current))       return StartToken(ScanState.InName);
                    else if (IsPunctuation(Current)) return StartToken(ScanState.InOperator);
                    else if (IsDigit(Current))       return StartToken(ScanState.InNumber);

                    Console.WriteLine("unexpected character");
                    return Eof;

                case ScanState.InString:
                    Advance();

                    if (Current == "\\") return ChangeState(ScanState.InStringEscape);
                    else if (Current == "\"")
                    {
                        Advance(); // eat the end quote
                        return CompleteToken(EscapeString);
                    }
                    else if (IsDone)
                    {
                        Console.WriteLine("error: source ended while still in string");
                        return Eof;
                    }

                    return null; // still in string

                case ScanState.InStringEscape:
                    Advance();

                    if (IsDone)
                    {
                        Console.WriteLine("error: source ended while still in string");
                        return Eof;
                    }
                    
                    return ChangeState(ScanState.InString);

                case ScanState.InMinus:
                    Advance();

                    // a "-" can be the start of an operator "-+!", a
                    // number "-123", or an operator all by itself "-"
                    if (IsPunctuation(Current) || IsAlpha(Current)) return ChangeState(ScanState.InOperator);
                    else if (IsDigit(Current)) return ChangeState(ScanState.InNumber);
                    
                    return CompleteToken((text, pos) => new Token(pos, TokenType.Operator, text));

                case ScanState.InName:
                    Advance();

                    if (!(IsAlpha(Current) || IsPunctuation(Current) || IsDigit(Current)))
                    {
                        return CompleteToken((text, pos) => new Token(pos, TokenType.Name, text));
                    }

                    return null; // still in identifier

                case ScanState.InOperator:
                    Advance();

                    if (!(IsAlpha(Current) || IsPunctuation(Current) || IsDigit(Current)))
                    {
                        return CompleteToken((text, pos) => new Token(pos, TokenType.Operator, text));
                    }

                    return null; // still in identifier

                case ScanState.InNumber:
                    Advance();

                    if (!IsDigit(Current)) return CompleteToken((text, pos) => new Token(pos, Int32.Parse(text)));

                    return null; // still in number

                case ScanState.InLineComment:
                    if (Match("\n"))
                    {
                        mState = ScanState.Default;
                        return new Token(LastChar, TokenType.Line);
                    }
                    else if (IsDone)
                    {
                        return Eof;
                    }
                    else
                    {
                        Advance(); // ignore everything else
                        return null;
                    }

                case ScanState.InBlockComment:
                    if (Match("/", "*"))
                    {
                        mBlockCommentDepth++;
                    }
                    else if (Match("*", "/"))
                    {
                        if (mBlockCommentDepth > 1)
                        {
                            mBlockCommentDepth--; // pop a level of nesting
                        }
                        else
                        {
                            mState = ScanState.Default; // not in a comment anymore
                        }
                    }
                    else Advance(); // ignore everything else

                    return null;

                default:
                    throw new Exception();
            }
        }

        private Token Eof
        {
            get
            {
                return new Token(new Position(mFile, mLine, mColumn, 0), TokenType.Eof);
            }
        }

        private Position LastChar
        {
            get { return new Position(mFile, mLine, mColumn - 1, 1); }
        }

        private Position Last2Chars
        {
            get { return new Position(mFile, mLine, mColumn - 2, 2); }
        }

        // Advances the scanner to the next character.
        private void Advance()
        {
            if (Current == "\n")
            {
                mLine++;
                mColumn = 0;
            }
            else
            {
                mColumn++;
            }

            mIndex++;
        }
    
        // Marks the next multi-character token as starting at the current position
        // then switches to the given state.
        private Token StartToken(ScanState newState)
        {
            mTokenStart = mIndex;
            mState = newState;
            mBlockCommentDepth = 1;

            return null;
        }
    
        // Switches to the given state without resetting the multi-character token
        // start position.
        private Token ChangeState(ScanState newState)
        {
            mState = newState;
            return null;
        }
  
        // Emits the current character range as a token using the given conversion
        // function, and then reverts back to the default state.
        private Token CompleteToken(Func<string, Position, Token> callback)
        {
            mState = ScanState.Default;
            return callback(mSource.Substring(mTokenStart, mIndex - mTokenStart), new Position(mFile, mLine, mColumn, mIndex - mTokenStart));
        }
    
        private Token EscapeString(string text, Position position)
        {
            string result   = "";

            bool   inEscape = false;
            int    index    = 1; // skip starting "
        
            while (index < (text.Length - 1)) // - 1 to skip ending "
            {
                string c = text.Substring(index, 1);
                if (inEscape)
                {
                    if      (c == "\"") result += "\"";
                    else if (c == "\\") result += "\\";
                    else if (c == "n")  result += "\n";
                    else if (c == "r")  result += "\r";
                    else Console.WriteLine("unknown escape char " + c);

                    inEscape = false;
                }
                else
                {
                    if (c == "\\") inEscape = true;
                    else result += c;
                }

                index++;
            }

            return new Token(position, result);
        }

        // Attempts to match the current character with the given one. Consumes it
        // and returns true if successful.
        private bool Match(string current)
        {
            if (Current == current)
            {
                // consume the character
                Advance();

                return true;
            }

            return false;
        }

        // Attempts to match the next two characters with the given pair. Consumes
        // them and returns true if successful.
        private bool Match (string current, string next)
        {
            if ((Current == current) && (Next == next))
            {
                // consume the characters
                Advance();
                Advance();

                return true;
            }

            return false;
        }

        private string Current
        {
            get
            {
                if (mIndex < mSource.Length) return mSource.Substring(mIndex, 1);
                return "end";
            }
        }
    
        private string Next
        {
            get
            {
                if (mIndex < mSource.Length - 1) return mSource.Substring(mIndex + 1, 1);
                return "end";
            }
        }

        private bool IsDigit(string text) { return "0123456789".Contains(text); }
        private bool IsAlpha(string text) { return "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".Contains(text); }
        private bool IsPunctuation(string text) { return "!&|-+=<>?*/~@#$%^`".Contains(text); }

        private bool IsDone { get { return Current == "end"; } }

        #region IEnumerable<Token> Members

        public IEnumerator<Token> GetEnumerator()
        {
            Token token;
            do
            {
                token = Scan();
                yield return token;
            }
            while (token.Type != TokenType.Eof);
        }

        #endregion

        #region IEnumerable Members

        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        #endregion

        private readonly string mSource;

        private ScanState mState = ScanState.Default;
        private int mIndex;
        private int mTokenStart;
        private int mBlockCommentDepth;

        private string mFile;
        private int mLine = 1;
        private int mColumn = 0;
    }
}
