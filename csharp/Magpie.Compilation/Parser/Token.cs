using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public enum TokenType
    {
        LeftParen,
        RightParen,
        LeftBracket,
        RightBracket,
        LeftArrow,
        RightArrow,
        Comma,
        Colon,
        Dot,
        Line,
        Prime, // '

        // keywords
        Def,
        Do,
        Else,
        End,
        Fn,
        If,
        Mutable,
        Namespace,
        Struct,
        Then,
        Union,
        Using,
        While,
        
        // literals
        Bool,
        Int,
        String,
        
        // identifiers
        Name,
        Operator,
        
        // the end of the source has been reached
        Eof
    }

    public class TokenPosition
    {
        public int Line { get; private set; }
        public int Column { get; private set; }
        public int Length { get; private set; }

        public TokenPosition(int line, int column, int length)
        {
            Line = line;
            Column = column;
            Length = length;
        }
    }

    public class Token
    {
        public TokenPosition Position;
        public TokenType Type;
        public bool BoolValue;
        public int IntValue;
        public string StringValue;

        public Token(TokenPosition position, TokenType type)
            : this(position)
        {
            Type = type;
        }

        public Token(TokenPosition position, TokenType type, string text)
            : this(position)
        {
            Type = type;
            StringValue = text;
        }

        public Token(TokenPosition position, bool value)
            : this(position, TokenType.Bool)
        {
            BoolValue = value;
        }

        public Token(TokenPosition position, int value)
            : this(position, TokenType.Int)
        {
            IntValue = value;
        }

        public Token(TokenPosition position, string value)
            : this(position, TokenType.String)
        {
            StringValue = value;
        }

        private Token(TokenPosition position)
        {
            Position = position;
        }

        public override string ToString()
        {
            switch (Type)
            {
                case TokenType.LeftParen:     return "(";
                case TokenType.RightParen:    return ")";
                case TokenType.LeftBracket:   return "[";
                case TokenType.RightBracket:  return "]";
                case TokenType.LeftArrow:     return "<-";
                case TokenType.RightArrow:    return "->";
                case TokenType.Comma:         return ",";
                case TokenType.Colon:         return ":";
                case TokenType.Dot:           return ".";
                case TokenType.Line:          return "newline";
                
                case TokenType.Name:          return "name " + StringValue;
                case TokenType.Operator:      return "operator " + StringValue;

                case TokenType.Def:         return "def";
                case TokenType.Do:          return "do";
                case TokenType.Else:        return "else";
                case TokenType.End:         return "end";
                case TokenType.Fn:          return "fn";
                case TokenType.If:          return "if";
                case TokenType.Mutable:     return "mutable";
                case TokenType.Namespace:   return "namespace";
                case TokenType.Struct:      return "struct";
                case TokenType.Then:        return "then";
                case TokenType.Union:       return "union";
                case TokenType.Using:       return "using";
                case TokenType.While:       return "while";

                case TokenType.Bool:        return "bool " + BoolValue;
                case TokenType.Int:         return "int " + IntValue;
                case TokenType.String:      return "string " + StringValue;

                case TokenType.Eof:         return "[end]";
                default: throw new Exception();
            }
        }
    }
}
