#include <iostream> // for debugging
#include <cstdlib>
#include <cstring>

#include "Array.h"
#include "Lexer.h"

namespace magpie
{
  gc<Token> Lexer::readToken()
  {
    while (true)
    {
      gc<Token> token = readRawToken();

      switch (token->type())
      {
        // Ignore newlines after tokens that can't end an expression.
        case TOKEN_LEFT_PAREN:
        case TOKEN_LEFT_BRACKET:
        case TOKEN_LEFT_BRACE:
        case TOKEN_COMMA:
        case TOKEN_EQ:
        case TOKEN_EQEQ:
        case TOKEN_NEQ:
        case TOKEN_LT:
        case TOKEN_GT:
        case TOKEN_LTE:
        case TOKEN_GTE:
        case TOKEN_PLUS:
        case TOKEN_MINUS:
        case TOKEN_STAR:
        case TOKEN_SLASH:
        case TOKEN_PERCENT:
        case TOKEN_AND:
        case TOKEN_IS:
        case TOKEN_NOT:
        case TOKEN_OR:
        case TOKEN_XOR:
          skipNewline_ = true;
          break;

        // TODO(bob): Need to decide how we want to handle keywords. Some of
        // them specifically should *not* elide newlines because they can have
        // a block after them.
        /*
        case TOKEN_CASE:
        case TOKEN_DEF:
        case TOKEN_DO:
        case TOKEN_END:
        case TOKEN_ELSE:
        case TOKEN_FALSE:
        case TOKEN_FOR:
        case TOKEN_IF:
        case TOKEN_MATCH:
        case TOKEN_RETURN:
        case TOKEN_THEN:
        case TOKEN_TRUE:
        case TOKEN_VAL:
        case TOKEN_VAR:
        case TOKEN_WHILE:
        */

        case TOKEN_LINE:
          if (skipNewline_) continue;

          // Collapse multiple newlines into one.
          skipNewline_ = true;
          break;

        default:
          // A line after any other token is significant.
          skipNewline_ = false;
          break;
      }

      return token;
    }
  }

  gc<Token> Lexer::readRawToken()
  {
    while (true)
    {
      if (isDone()) return makeToken(TOKEN_EOF, String::create(""));

      start_ = pos_;
      startRow_ = currentRow_;
      startCol_ = currentCol_;

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
        case ',': return makeToken(TOKEN_COMMA);
        case '=':
          if (peek() == '=')
          {
            advance();
            return makeToken(TOKEN_EQEQ);
          }
          return makeToken(TOKEN_EQ);
          
        case '!':
          if (peek() == '=')
          {
            advance();
            return makeToken(TOKEN_NEQ);
          }
          return error(String::create("Expect '=' after '!'."));
          
        case '<':
          if (peek() == '=')
          {
            advance();
            return makeToken(TOKEN_LTE);
          }
          return makeToken(TOKEN_LT);
          
        case '>':
          if (peek() == '=')
          {
            advance();
            return makeToken(TOKEN_GTE);
          }
          return makeToken(TOKEN_GT);
          
        case '+': return makeToken(TOKEN_PLUS);
        case '-': return makeToken(TOKEN_MINUS);
        case '*': return makeToken(TOKEN_STAR);
        case '%': return makeToken(TOKEN_PERCENT);
        case '\n': return makeToken(TOKEN_LINE);

        case '/':
          if (peek() == '/')
          {
            skipLineComment();
          }
          else if (peek() == '*')
          {
            skipBlockComment();
          }
          else
          {
            return makeToken(TOKEN_SLASH);
          }
          break;

        case '"': return readString();

        default:
          if (isNameStart(c)) return readName();
          if (isDigit(c)) return readNumber();

          // If we got here, we don't know what it is.
          return error(String::format("Unknown character '%c'.", c));
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

    if (!isDone())
    {
      pos_++;

      if (c == '\n')
      {
        currentRow_++;
        currentCol_ = 1;
      }
      else
      {
        currentCol_++;
      }
    }

    return c;
  }

  gc<Token> Lexer::makeToken(TokenType type)
  {
    return makeToken(type, source_->substring(start_, pos_));
  }

  gc<Token> Lexer::makeToken(TokenType type, gc<String> text)
  {
    SourcePos pos = SourcePos(fileName_,
        startRow_, startCol_, currentRow_, currentCol_);
    return new Token(type, text, pos);
  }

  gc<Token> Lexer::error(gc<String> message)
  {
    return makeToken(TOKEN_ERROR, message);
  }

  void Lexer::skipLineComment()
  {
    while (!isDone() && peek() != '\n') advance();
  }

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
      else
      {
        advance();
      }
    }
  }

  gc<Token> Lexer::readName()
  {
    while (isName(peek())) advance();

    gc<String> text = source_->substring(start_, pos_);
    
    // See if it's a field.
    if (peek() == ':')
    {
      advance();
      return makeToken(TOKEN_FIELD, text);
    }
    
    // See if it's a reserved word.
    TokenType type = TOKEN_NAME;
    if      (*text == "and"    ) type = TOKEN_AND;
    else if (*text == "case"   ) type = TOKEN_CASE;
    else if (*text == "catch"  ) type = TOKEN_CATCH;
    else if (*text == "def"    ) type = TOKEN_DEF;
    else if (*text == "do"     ) type = TOKEN_DO;
    else if (*text == "else"   ) type = TOKEN_ELSE;
    else if (*text == "end"    ) type = TOKEN_END;
    else if (*text == "false"  ) type = TOKEN_FALSE;
    else if (*text == "for"    ) type = TOKEN_FOR;
    else if (*text == "if"     ) type = TOKEN_IF;
    else if (*text == "is"     ) type = TOKEN_IS;
    else if (*text == "match"  ) type = TOKEN_MATCH;
    else if (*text == "not"    ) type = TOKEN_NOT;
    else if (*text == "nothing") type = TOKEN_NOTHING;
    else if (*text == "or"     ) type = TOKEN_OR;
    else if (*text == "return" ) type = TOKEN_RETURN;
    else if (*text == "then"   ) type = TOKEN_THEN;
    else if (*text == "throw"  ) type = TOKEN_THROW;
    else if (*text == "true"   ) type = TOKEN_TRUE;
    else if (*text == "val"    ) type = TOKEN_VAL;
    else if (*text == "var"    ) type = TOKEN_VAR;
    else if (*text == "while"  ) type = TOKEN_WHILE;
    else if (*text == "xor"    ) type = TOKEN_XOR;

    return makeToken(type, text);
  }

  gc<Token> Lexer::readNumber()
  {
    while (isDigit(peek())) advance();
    
    // See if it's a field.
    if (peek() == ':')
    {
      gc<String> text = source_->substring(start_, pos_);
      
      advance();
      return makeToken(TOKEN_FIELD, text);
    }
    
    // Read the fractional part, if any.
    if (peek() == '.')
    {
      advance();
      while (isDigit(peek())) advance();
    }

    return makeToken(TOKEN_NUMBER);
  }

  gc<Token> Lexer::readString()
  {
    Array<char> chars;
    while (true)
    {
      if (isDone())
      {
        return error(String::create("Unterminated string."));
      }

      char c = advance();
      if (c == '"')
      {
        gc<String> text = String::create(chars);
        return makeToken(TOKEN_STRING, text);
      }

      // An escape sequence.
      if (c == '\\')
      {
        if (isDone())
        {
          return error(String::create("Unterminated string escape."));
        }

        char e = advance();
        switch (e)
        {
          case 'n':  chars.add('\n'); break;
          case '"':  chars.add('"'); break;
          case '\\': chars.add('\\'); break;
          case 't':  chars.add('\t'); break;
          default:
            return error(String::format("Unknown escape sequence '%c'.", e));
        }
      }
      else
      {
        // Normal character.
        chars.add(c);
      }
    }
  }
}
