#include "Token.h"
#include "Memory.h"

namespace magpie
{
  SourceFile::SourceFile(gc<String> path, gc<String> source)
  : path_(path),
    source_(source)
  {}

  gc<String> SourceFile::getLine(int line)
  {
    // TODO(bob): Cache this for perf?
    int currentLine = 1;
    int start = 0;
    int end = source_->length();
    for (int i = 0; i < source_->length(); i++)
    {
      if ((*source_)[i] == '\n')
      {
        currentLine++;

        if (currentLine == line)
        {
          start = i + 1;
        }
        else if (currentLine == line + 1)
        {
          end = i;
          break;
        }
      }
    }

    // If we're getting the very last line and it's empty (i.e. the source file
    // ends with a newline), then return an empty string.
    if (start >= source_->length()) return String::create("");

    return source_->substring(start, end);
  }

  void SourceFile::reach()
  {
    path_.reach();
    source_.reach();
  }

  SourcePos::SourcePos(gc<SourceFile> file, int startLine, int startCol,
                       int endLine, int endCol)
  : file_(file),
    startLine_(startLine),
    startCol_(startCol),
    endLine_(endLine),
    endCol_(endCol)
    {}

  gc<SourcePos> SourcePos::spanTo(gc<SourcePos> end) const
  {
    return new SourcePos(file_, startLine_, startCol_,
                         end->endLine_, end->endCol_);
  }

  void SourcePos::reach()
  {
    file_.reach();
  }

  Token::Token(TokenType type, gc<String> text, gc<SourcePos> pos)
  : type_(type),
    text_(text),
    pos_(pos)
  {}

  const char* Token::typeString(TokenType type)
  {
    switch (type)
    {
        // Punctuators.
      case TOKEN_LEFT_PAREN:    return "(";
      case TOKEN_RIGHT_PAREN:   return ")";
      case TOKEN_LEFT_BRACKET:  return "[";
      case TOKEN_RIGHT_BRACKET: return "]";
      case TOKEN_LEFT_BRACE:    return "{";
      case TOKEN_RIGHT_BRACE:   return "}";
      case TOKEN_COMMA:         return ",";
      case TOKEN_DOT:           return ".";
      case TOKEN_DOTDOT:        return "..";
      case TOKEN_DOTDOTDOT:     return "...";
      case TOKEN_EQ:            return "=";
      case TOKEN_EQEQ:          return "==";
      case TOKEN_NEQ:           return "!=";
      case TOKEN_COMPARE_OP:    return "compare op";
      case TOKEN_TERM_OP:       return "term op";
      case TOKEN_PRODUCT_OP:    return "product op";

        // Keywords.
      case TOKEN_AND:           return "and";
      case TOKEN_AS:            return "as";
      case TOKEN_ASYNC:         return "async";
      case TOKEN_BREAK:         return "break";
      case TOKEN_CASE:          return "case";
      case TOKEN_CATCH:         return "catch";
      case TOKEN_DEF:           return "def";
      case TOKEN_DEFCLASS:      return "defclass";
      case TOKEN_DO:            return "do";
      case TOKEN_DONE:          return "done";
      case TOKEN_END:           return "end";
      case TOKEN_ELSE:          return "else";
      case TOKEN_FALSE:         return "false";
      case TOKEN_FN:            return "fn";
      case TOKEN_FOR:           return "for";
      case TOKEN_IF:            return "if";
      case TOKEN_IMPORT:        return "import";
      case TOKEN_IN:            return "in";
      case TOKEN_IS:            return "is";
      case TOKEN_MATCH:         return "match";
      case TOKEN_NOT:           return "not";
      case TOKEN_NOTHING:       return "nothing";
      case TOKEN_OR:            return "or";
      case TOKEN_RETURN:        return "return";
      case TOKEN_THEN:          return "then";
      case TOKEN_THROW:         return "throw";
      case TOKEN_TRUE:          return "true";
      case TOKEN_VAL:           return "val";
      case TOKEN_VAR:           return "var";
      case TOKEN_WHILE:         return "while";
      case TOKEN_XOR:           return "xor";

      case TOKEN_FIELD:         return "field";
      case TOKEN_NAME:          return "name";
      case TOKEN_CHARACTER:     return "character";
      case TOKEN_FLOAT:         return "float";
      case TOKEN_INT:           return "int";
      case TOKEN_STRING:        return "string";

      case TOKEN_LINE:          return "line";
      case TOKEN_ERROR:         return "error";
      case TOKEN_EOF:           return "eof";
      default:
        ASSERT(false, "Unknown TokenType.");
    }
  }

  void Token::reach()
  {
    text_.reach();
  }

  void Token::trace(std::ostream& out) const
  {
    switch (type_)
    {
      case TOKEN_NAME:
      case TOKEN_CHARACTER:
      case TOKEN_FLOAT:
      case TOKEN_INT:
      case TOKEN_STRING:
      case TOKEN_ERROR:
        // Show the text.
        out << text_;
        break;

      case TOKEN_FIELD:
        out << text_ << ":";
        break;

      default:
        // It's a token type with a fixed text, so just use that.
        out << typeString(type_);
    }
  }
}
