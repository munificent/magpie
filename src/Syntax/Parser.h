#pragma once

#include "Common.h"
#include "Data/Queue.h"
#include "Syntax/ErrorReporter.h"
#include "Syntax/Lexer.h"

namespace magpie
{
  class Lexer;
  class ErrorReporter;
  class Expr;

  // Parses Magpie source from a string into an abstract syntax tree. The
  // implementation is basically a vanilla recursive descent parser wrapped
  // around a Pratt operator precedence parser for handling expressions.
  class Parser
  {
  public:
    Parser(gc<SourceFile> source, ErrorReporter& reporter)
    : lexer_(source),
      reporter_(reporter),
      read_(),
      last_()
    {}

    // Parses an entire module.
    gc<ModuleAst> parseModule();

    // Parses a single expression in a REPL session.
    gc<Expr> parseExpression();

  private:
    typedef gc<Expr> (Parser::*PrefixParseFn)(gc<Token> token);
    typedef gc<Expr> (Parser::*InfixParseFn)(gc<Expr> left, gc<Token> token);

    struct Parselet
    {
      PrefixParseFn prefix;
      InfixParseFn  infix;
      int           precedence;
    };

    gc<Expr> parseBlock(TokenType endToken = TOKEN_END);
    gc<Expr> parseBlock(TokenType end1, TokenType end2, TokenType end3,
                        TokenType* outEndToken);
    gc<Expr> parseBlock(TokenType end1, TokenType end2,
                        TokenType* outEndToken);
    gc<Expr> parseBlock(bool allowCatch, TokenType end1, TokenType end2,
                        TokenType end3, TokenType* outEndToken);
    gc<Expr> maybeImport();
    gc<Expr> topLevelExpression();
    gc<Expr> statement(bool allowBlockArgument);
    gc<Expr> flowControl(bool allowBlockArgument);

    // Parses an expression with the given precedence or higher.
    gc<Expr> parsePrecedence(int precedence = 0);

    // Prefix expression parsers.
    gc<Expr> boolean(gc<Token> token);
    gc<Expr> character(gc<Token> token);
    gc<Expr> done(gc<Token> token);
    gc<Expr> float_(gc<Token> token);
    gc<Expr> function(gc<Token> token);
    gc<Expr> group(gc<Token> token);
    gc<Expr> int_(gc<Token> token);
    gc<Expr> list(gc<Token> token);
    gc<Expr> name(gc<Token> token);
    gc<Expr> not_(gc<Token> token);
    gc<Expr> nothing(gc<Token> token);
    gc<Expr> record(gc<Token> token);
    gc<Expr> string(gc<Token> token);
    gc<Expr> throw_(gc<Token> token);

    // Infix expression parsers.
    gc<Expr> and_(gc<Expr> left, gc<Token> token);
    gc<Expr> assignment(gc<Expr> left, gc<Token> token);
    gc<Expr> call(gc<Expr> left, gc<Token> token);
    gc<Expr> infixCall(gc<Expr> left, gc<Token> token);
    gc<Expr> infixRecord(gc<Expr> left, gc<Token> token);
    gc<Expr> is(gc<Expr> left, gc<Token> token);
    gc<Expr> or_(gc<Expr> left, gc<Token> token);
    gc<Expr> prefixCall(gc<Token> token);
    gc<Expr> subscript(gc<Expr> left, gc<Token> token);

    // Pattern parsing.
    gc<Pattern> parsePattern(bool isMethod);
    gc<Pattern> recordPattern(bool isMethod);
    gc<Pattern> variablePattern(bool isMethod);
    gc<Pattern> primaryPattern(bool isMethod);
    gc<Expr> parseExpressionInPattern(bool isMethod);

    // Create an appropriate pattern for binding a function's expected
    // parameters to the non-destructured single argument it will actually
    // receive.
    gc<Pattern> expandFunctionPattern(gc<SourcePos> pos, gc<Pattern> pattern);

    // The left-hand side of an assignment expression is an lvalue, but it will
    // initially be parsed as an expression. Correctly determining whether a
    // series of tokens is the LHS of an assignment before parsing them
    // requires arbitrary lookahead.
    //
    // Instead, the parser assumes it's parsing an expression until it hits an
    // '='. Then it takes the LHS expression and converts it to an lvalue.
    gc<LValue> convertToLValue(gc<Expr> expr);

    gc<Expr> createSequence(const Array<gc<Expr> >& exprs);

    // Gets the token the parser is currently looking at.
    const gc<Token> current();

    // Gets the most recently consumed token.
    const gc<Token> last() const { return last_; }

    // Returns true if the current token is the given type.
    bool lookAhead(TokenType type);

    // Returns true if the current and next tokens is the given types (in
    // order).
    bool lookAhead(TokenType current, TokenType next);

    // Consumes the current token and returns true if it is the given type,
    // otherwise returns false.
    bool match(TokenType type);

    // Verifies the current token if it matches the expected type, and
    // reports an error if it doesn't. Does not consume the token either
    // way.
    void expect(TokenType expected, const char* errorMessage);

    // Consumes the current token and advances the parser.
    gc<Token> consume();

    // Consumes the current token if it matches the expected type.
    // Otherwise reports the given error message and returns a null temp.
    gc<Token> consume(TokenType expected, const char* errorMessage);

    // Detects if the lexer ran out of input when another line is expected.
    // This lets the REPL know it needs to read another line.
    void checkForMissingLine();

    void fillLookAhead(int count);

    // Creates a [SourcePos] that spans the code starting at [from] up to the
    // last consumed [Token].
    gc<SourcePos> spanFrom(gc<Token> from);
    gc<SourcePos> spanFrom(gc<Expr> from);

    static Parselet expressions_[TOKEN_NUM_TYPES];

    Lexer lexer_;

    ErrorReporter& reporter_;

    // The 2 here is the maximum number of lookahead tokens.
    Queue<gc<Token>, 2> read_;

    // The most recently consumed token.
    gc<Token> last_;

    NO_COPY(Parser);
  };

  // Given the body of a "fn" expression without a parameter pattern, walks the
  // body to find implicit parameters ("_"). Returns a new FnExpr with a valid
  // pattern and with all occurrences of implicit parameters in the body
  // replaced with references to the generated parameters.
  class ImplicitParameterTransformer : public ExprVisitor /*, private LValueVisitor */
  {
  public:
    static void transform(gc<Expr>& body, gc<Pattern>& pattern);

  private:
    ImplicitParameterTransformer();

    virtual void visit(AndExpr& expr, int dummy);
    virtual void visit(AssignExpr& expr, int dest);
    virtual void visit(AsyncExpr& expr, int dummy);
    virtual void visit(AtomExpr& expr, int dummy);
    virtual void visit(BreakExpr& expr, int dummy);
    virtual void visit(CallExpr& expr, int dummy);
    virtual void visit(CatchExpr& expr, int dummy);
    virtual void visit(CharacterExpr& expr, int dummy);
    virtual void visit(DefExpr& expr, int dummy);
    virtual void visit(DefClassExpr& expr, int dummy);
    virtual void visit(DoExpr& expr, int dummy);
    virtual void visit(FloatExpr& expr, int dummy);
    virtual void visit(FnExpr& expr, int dummy);
    virtual void visit(ForExpr& expr, int dummy);
    virtual void visit(GetFieldExpr& expr, int dummy);
    virtual void visit(IfExpr& expr, int dummy);
    virtual void visit(ImportExpr& expr, int dummy);
    virtual void visit(IntExpr& expr, int dummy);
    virtual void visit(IsExpr& expr, int dummy);
    virtual void visit(ListExpr& expr, int dummy);
    virtual void visit(MatchExpr& expr, int dummy);
    virtual void visit(NameExpr& expr, int dummy);
    virtual void visit(NativeExpr& expr, int dest);
    virtual void visit(NotExpr& expr, int dummy);
    virtual void visit(OrExpr& expr, int dummy);
    virtual void visit(RecordExpr& expr, int dummy);
    virtual void visit(ReturnExpr& expr, int dummy);
    virtual void visit(SequenceExpr& expr, int dummy);
    virtual void visit(SetFieldExpr& expr, int dummy);
    virtual void visit(StringExpr& expr, int dummy);
    virtual void visit(ThrowExpr& expr, int dummy);
    virtual void visit(VariableExpr& expr, int dummy);
    virtual void visit(WhileExpr& expr, int dummy);

    /*
    virtual void visit(CallLValue& lvalue, int dummy);
    virtual void visit(NameLValue& lvalue, int dummy);
    virtual void visit(RecordLValue& lvalue, int dummy);
    virtual void visit(WildcardLValue& lvalue, int dummy);
    */

    void replace(gc<Expr> expr);
    gc<Expr> transform(gc<Expr> expr);

    int numParams_;
    Array<gc<Expr> > results_;

    NO_COPY(ImplicitParameterTransformer);
  };
}
