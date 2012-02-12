#include <iostream> // for debugging
#include <cstdlib>
#include <cstring>

#include "Lexer.h"

namespace magpie
{
  temp<Token> Lexer::readToken()
  {
    while (true)
    {
      if (isDone()) return Token::create(TOKEN_EOF, String::create(""));

      start_ = pos_;

      char c = advance();
      switch (c)
      {
        case ' ':
        case '\t':
        case '\r':
          // Skip whitespace.
          while (isWhitespace(peek())) advance();
          break;

        case '(': return makeToken(TOKEN_LEFT_PAREN);
        case ')': return makeToken(TOKEN_RIGHT_PAREN);
        case '[': return makeToken(TOKEN_LEFT_BRACKET);
        case ']': return makeToken(TOKEN_RIGHT_BRACKET);
        case '{': return makeToken(TOKEN_LEFT_BRACE);
        case '}': return makeToken(TOKEN_RIGHT_BRACE);
        case '=': return makeToken(TOKEN_EQUALS);
        case '+': return makeToken(TOKEN_PLUS);
        case '-': return makeToken(TOKEN_MINUS);
        case '*': return makeToken(TOKEN_STAR);
        case '%': return makeToken(TOKEN_PERCENT);
        case '<': return makeToken(TOKEN_LESS_THAN);

        case '\n': return makeToken(TOKEN_LINE);

        case '/':
          if (peek() == '/')
          {
            skipLineComment();
          /*}
            else if (peek() == '*')
          {
            skipBlockComment();*/
          }
          else
          {
            return makeToken(TOKEN_SLASH);
          }
          break;

          /*
        case ',': return singleToken(TOKEN_LINE);
        case '@': return singleToken(TOKEN_AT);
        case '.': return singleToken(TOKEN_DOT);
        case '#': return singleToken(TOKEN_HASH);
        case ';': return singleToken(TOKEN_SEMICOLON);
        case '\\': return singleToken(TOKEN_IGNORE_LINE);
        case '|': return singleToken(TOKEN_PIPE);

        case ':':
          advance();
          if (peek() == ':')
          {
            // "::".
            advance();
            return Ref<Token>(new Token(TOKEN_BIND));
          }

          // Just a ":" by itself.
          return Ref<Token>(new Token(TOKEN_KEYWORD, ":"));

        case '-':
          advance();
          if (isDigit(peek())) return readNumber();
          return readOperator();

        case '/':
          advance();
          if (peek() == '/')
          {
            // Line comment, so ignore the rest of the line and
            // emit the line token.
            mNeedsLine = true;
            return Ref<Token>(new Token(TOKEN_LINE));
          }
          else if (peek() == '*')
          {
            skipBlockComment();
          }
          else
          {
            return readOperator();
          }
          break;

        case '"': return readString();

        default:
          if (isDigit(c)) return readNumber();
          if (isOperator(c)) return readOperator();
           */
        default:
          if (isNameStart(c)) return readName();
          if (isDigit(c)) return readNumber();
          
          // If we got here, we don't know what it is.
          return makeToken(TOKEN_ERROR);
      }
    }
  }

  bool Lexer::isDone() const
  {
    return pos_ == source_->length();
  }

  bool Lexer::isWhitespace(char c) const
  {
    return (c == ' ') || (c == '\t') || (c == '\r');
  }

  bool Lexer::isNameStart(char c) const
  {
    return (c == '_') ||
          ((c >= 'a') && (c <= 'z')) ||
          ((c >= 'A') && (c <= 'Z'));
  }

  bool Lexer::isName(char c) const
  {
    return isNameStart(c) || isDigit(c);
  }

  bool Lexer::isDigit(char c) const
  {
    return (c >= '0') && (c <= '9');
  }

  char Lexer::peek(int ahead) const
  {
    if (pos_ + ahead >= source_->length()) return '\0';
    return (*source_)[pos_ + ahead];
  }

  char Lexer::advance()
  {
    char c = peek();
    pos_++;
    return c;
  }

  temp<Token> Lexer::makeToken(TokenType type)
  {
    return Token::create(type, source_->substring(start_, pos_));
  }

  void Lexer::skipLineComment()
  {
    // TODO(bob): Handle EOF.
    while (peek() != '\n') advance();
  }

  temp<Token> Lexer::readName()
  {
    // TODO(bob): Handle EOF.
    while (isName(peek())) advance();

    temp<String> text = source_->substring(start_, pos_);

    // See if it's a reserved word.
    TokenType type = TOKEN_NAME;
    if      (*text == "and"   ) type = TOKEN_AND;
    else if (*text == "case"  ) type = TOKEN_CASE;
    else if (*text == "def"   ) type = TOKEN_DEF;
    else if (*text == "do"    ) type = TOKEN_DO;
    else if (*text == "else"  ) type = TOKEN_ELSE;
    else if (*text == "end"   ) type = TOKEN_END;
    else if (*text == "false" ) type = TOKEN_FALSE;
    else if (*text == "for"   ) type = TOKEN_FOR;
    else if (*text == "if"    ) type = TOKEN_IF;
    else if (*text == "is"    ) type = TOKEN_IS;
    else if (*text == "match" ) type = TOKEN_MATCH;
    else if (*text == "not"   ) type = TOKEN_NOT;
    else if (*text == "or"    ) type = TOKEN_OR;
    else if (*text == "return") type = TOKEN_RETURN;
    else if (*text == "then"  ) type = TOKEN_THEN;
    else if (*text == "true"  ) type = TOKEN_TRUE;
    else if (*text == "val"   ) type = TOKEN_VAL;
    else if (*text == "var"   ) type = TOKEN_VAR;
    else if (*text == "while" ) type = TOKEN_WHILE;
    else if (*text == "xor"   ) type = TOKEN_XOR;

    return Token::create(type, text);
  }

  temp<Token> Lexer::readNumber()
  {
    // TODO(bob): Handle EOF.
    while (isDigit(peek())) advance();

    // Read the fractional part, if any.
    if (peek() == '.')
    {
      advance();
      while (isDigit(peek())) advance();
    }

    return makeToken(TOKEN_NUMBER);
  }

  /*
  void Lexer::skipBlockComment()
  {
    advance();
    advance();

    int nesting = 1;

    while (nesting > 0)
    {
      // TODO(bob): Unterminated comment. Should return error.
      if (isDone()) return;

      if ((peek() == '/') && (peek(1) == '*'))
      {
        advance();
        advance();
        nesting++;
      }
      else if ((peek() == '*') && (peek(1) == '/'))
      {
        advance();
        advance();
        nesting--;
      }
      else if (peek() == '\0')
      {
        advanceLine();
      }
      else
      {
        advance();
      }
    }
  }

  Ref<Token> Lexer::readString()
  {
    advance();

    String text;
    while (true)
    {
      if (isDone()) return Ref<Token>(new Token(TOKEN_ERROR, "Unterminated string."));

      char c = advance();
      if (c == '"') return Ref<Token>(new Token(TOKEN_STRING, text));

      // An escape sequence.
      if (c == '\\')
      {
        if (isDone()) return Ref<Token>(new Token(TOKEN_ERROR,
                                                  "Unterminated string escape."));

        char e = advance();
        switch (e)
        {
          case 'n': text += "\n"; break;
          case '"': text += "\""; break;
          case '\\': text += "\\"; break;
          case 't': text += "\t"; break;
          default:
            return Ref<Token>(new Token(TOKEN_ERROR, String::Format(
                                                                    "Unrecognized escape sequence \"%c\".", e)));
        }
      }
      else
      {
        // Normal character.
        text += c;
      }
    }
  }
  */
}
