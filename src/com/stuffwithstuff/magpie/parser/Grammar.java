package com.stuffwithstuff.magpie.parser;

import java.util.HashSet;
import java.util.Set;

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
    
    prefix("fn",        new FnParser());

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

    reserve("break case catch def defclass do else end export for");
    reserve("import if in is match return then throw val var while");
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
  
  public void defineParser(String keyword, PrefixParser parser) {
    mPrefixParsers.define(keyword, parser);
  }
  
  public void defineParser(String keyword, InfixParser parser) {
    mInfixParsers.define(keyword, parser);
  }
  
  public boolean isKeyword(String name) {
    return mReservedWords.contains(name) ||
      mPrefixParsers.contains(name) ||
      mInfixParsers.contains(name);
  }
  /**
   * Gets whether or not this token is a reserved word. Reserved words like
   * "else" and "then" are claimed for special use by mixfix parsers, so can't
   * be parsed on their own.
   * 
   * @param   token  The token to test
   * @return         True if the token is a reserved name token.
   */
  public boolean isReserved(String name) {
    return mReservedWords.contains(name);
  }
  
  /**
   * Gets whether or not this token is a reserved word. Reserved words like
   * "else" and "then" are claimed for special use by mixfix parsers, so can't
   * be parsed on their own.
   * 
   * @param   token  The token to test
   * @return         True if the token is a reserved name token.
   */
  public boolean isReserved(Token token) {
    return (token.getType() == TokenType.NAME) &&
        isReserved(token.getString());
  }
  
  public int getPrecedence(Token token) {
    int precedence = 0;
    
    // A reserved word can't start an infix expression. Prevents us from
    // parsing a keyword as an identifier.
    if (isReserved(token)) return 0;
    
    // If we have a prefix parser for this token's name, then that takes
    // precedence. Prevents us from parsing a keyword as an identifier.
    if ((token.getValue() instanceof String) &&
        mPrefixParsers.isReserved(token.getString())) return 0;

    InfixParser parser = mInfixParsers.get(token);
    if (parser != null) {
      precedence = parser.getPrecedence();
    }
    
    return precedence;
  }
  
  private void prefix(TokenType type, PrefixParser parser) {
    mPrefixParsers.define(type, parser);
  }
  
  private void prefix(String keyword, PrefixParser parser) {
    mReservedWords.add(keyword);
    mPrefixParsers.define(keyword, parser);
  }
  
  private void infix(TokenType type, InfixParser parser) {
    mInfixParsers.define(type, parser);
  }
  
  private void reserve(String wordString) {
    String[] words = wordString.split(" ");
    for (int i = 0; i < words.length; i++) {
      mReservedWords.add(words[i]);
    }
  }
  
  private final ParserTable<PrefixParser> mPrefixParsers =
      new ParserTable<PrefixParser>();
  private final ParserTable<InfixParser> mInfixParsers =
      new ParserTable<InfixParser>();
  private final Set<String> mReservedWords = new HashSet<String>();
}
