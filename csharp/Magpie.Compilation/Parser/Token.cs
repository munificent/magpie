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
        RightBracketBang, // ]! used for mutable arrays
        LeftCurly,
        RightCurly,
        LeftArrow,
        RightArrow,
        Comma,
        Colon,
        Dot,
        Line,
        Prime, // '

        // keywords
        Case,
        Def,
        Do,
        Else,
        End,
        Fn,
        For,
        If,
        Let,
        Match,
        Namespace,
        Return,
        Struct,
        Then,
        Union,
        Using,
        Var,
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

    public class Token
    {
        public Position Position;
        public TokenType Type;
        public bool BoolValue;
        public int IntValue;
        public string StringValue;

        public Token(Position position, TokenType type)
            : this(position)
        {
            Type = type;
        }

        public Token(Position position, TokenType type, string text)
            : this(position)
        {
            Type = type;
            StringValue = text;
        }

        public Token(Position position, bool value)
            : this(position, TokenType.Bool)
        {
            BoolValue = value;
        }

        public Token(Position position, int value)
            : this(position, TokenType.Int)
        {
            IntValue = value;
        }

        public Token(Position position, string value)
            : this(position, TokenType.String)
        {
            StringValue = value;
        }

        private Token(Position position)
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
                case TokenType.RightBracketBang:  return "]!";
                case TokenType.LeftCurly:     return "{";
                case TokenType.RightCurly:    return "}";
                case TokenType.LeftArrow:     return "<-";
                case TokenType.RightArrow:    return "->";
                case TokenType.Comma:         return ",";
                case TokenType.Colon:         return ":";
                case TokenType.Dot:           return ".";
                case TokenType.Line:          return "newline";
                
                case TokenType.Name:          return "name " + StringValue;
                case TokenType.Operator:      return "operator " + StringValue;

                case TokenType.Case:        return "case";
                case TokenType.Def:         return "def";
                case TokenType.Do:          return "do";
                case TokenType.Else:        return "else";
                case TokenType.End:         return "end";
                case TokenType.Fn:          return "fn";
                case TokenType.For:         return "for";
                case TokenType.If:          return "if";
                case TokenType.Let:         return "let";
                case TokenType.Match:       return "match";
                case TokenType.Namespace:   return "namespace";
                case TokenType.Struct:      return "struct";
                case TokenType.Then:        return "then";
                case TokenType.Union:       return "union";
                case TokenType.Using:       return "using";
                case TokenType.Var:         return "var";
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
