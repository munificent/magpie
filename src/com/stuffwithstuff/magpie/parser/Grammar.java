package com.stuffwithstuff.magpie.parser;

import java.util.HashMap;
import java.util.Map;

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
    return mPrefixParsers.get(token.getType());
  }

  public InfixParser getInfixParser(Token token) {
    return mInfixParsers.get(token.getType());
  }

  public int getPrecedence(Token token) {
    int precedence = 0;

    InfixParser parser = mInfixParsers.get(token.getType());
    if (parser != null) {
      precedence = parser.getPrecedence();
    }
    
    return precedence;
  }
  
  private void prefix(TokenType type, PrefixParser parser) {
    mPrefixParsers.put(type, parser);
  }
  
  private void infix(TokenType type, InfixParser parser) {
    mInfixParsers.put(type, parser);
  }
  
  private final Map<TokenType, PrefixParser> mPrefixParsers =
      new HashMap<TokenType, PrefixParser>();
  private final Map<TokenType, InfixParser> mInfixParsers =
      new HashMap<TokenType, InfixParser>();
}
