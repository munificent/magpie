package com.stuffwithstuff.magpie.parser;

public class Grammar {
  public Grammar() {
    prefix(TokenType.BOOL,          new LiteralParser());
    prefix(TokenType.INT,           new LiteralParser());
    prefix(TokenType.NOTHING,       new LiteralParser());
    prefix(TokenType.STRING,        new LiteralParser());
    prefix(TokenType.LEFT_PAREN,    new ParenthesisPrefixParser());
    prefix(TokenType.LEFT_BRACKET,  new BracketPrefixParser());
    prefix(TokenType.LEFT_BRACE,    new BracePrefixParser());
    prefix(TokenType.BACKTICK,      new BacktickParser());
    prefix(TokenType.NAME,          new NameParser());
    prefix(TokenType.FIELD,         new FieldParser());
    
    prefix(TokenType.FN,            new FnParser());

    infix(TokenType.AND,          new AndParser());
    infix(TokenType.OR,           new OrParser());
    infix(TokenType.NAME,         new NameParser());
    infix(TokenType.COMMA,        new CommaParser());
    infix(TokenType.EQ,           new EqualsParser());
    infix(TokenType.LEFT_BRACKET, new BracketInfixParser());

    infix(TokenType.ASTERISK,     new InfixOperatorParser(8));
    infix(TokenType.SLASH,        new InfixOperatorParser(8));
    infix(TokenType.PERCENT,      new InfixOperatorParser(8));
    infix(TokenType.PLUS,         new InfixOperatorParser(7));
    infix(TokenType.MINUS,        new InfixOperatorParser(7));
    infix(TokenType.LT,           new InfixOperatorParser(5));
    infix(TokenType.GT,           new InfixOperatorParser(5));
    infix(TokenType.LTE,          new InfixOperatorParser(5));
    infix(TokenType.GTE,          new InfixOperatorParser(5));
    infix(TokenType.EQEQ,         new InfixOperatorParser(4));
    infix(TokenType.NOTEQ,        new InfixOperatorParser(4));
  }
  
  public PrefixParser getPrefixParser(Token token) {
    return mPrefixParsers.get(token);
  }

  public PrefixParser getPrefixParser(String keyword) {
    return mPrefixParsers.get(keyword);
  }

  public InfixParser getInfixParser(Token token) {
    return mInfixParsers.get(token);
  }

  public InfixParser getInfixParser(String keyword) {
    return mInfixParsers.get(keyword);
  }
  
  public int getPrecedence(Token token) {
    int precedence = 0;

    InfixParser parser = mInfixParsers.get(token);
    if (parser != null) {
      precedence = parser.getPrecedence();
    }
    
    return precedence;
  }
  
  private void prefix(TokenType type, PrefixParser parser) {
    mPrefixParsers.define(type, parser);
  }
  
  private void infix(TokenType type, InfixParser parser) {
    mInfixParsers.define(type, parser);
  }
  
  private final ParserTable<PrefixParser> mPrefixParsers =
      new ParserTable<PrefixParser>();
  private final ParserTable<InfixParser> mInfixParsers =
      new ParserTable<InfixParser>();
}
