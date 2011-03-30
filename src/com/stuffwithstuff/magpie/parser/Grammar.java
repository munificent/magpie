package com.stuffwithstuff.magpie.parser;

import java.util.HashSet;
import java.util.Set;

public class Grammar {
  public Grammar() {
    // Register the built-in parsers.
    mPrefixParsers.define(TokenType.BOOL, new LiteralParser());
    mPrefixParsers.define(TokenType.INT, new LiteralParser());
    mPrefixParsers.define(TokenType.STRING, new LiteralParser());
    mPrefixParsers.define(TokenType.LEFT_PAREN, new ParenthesisPrefixParser());
    mPrefixParsers.define(TokenType.LEFT_BRACE, new BraceParser());
    mPrefixParsers.define(TokenType.BACKTICK, new BacktickParser());
    mPrefixParsers.define(TokenType.NAME, new MessagePrefixParser());
    mPrefixParsers.define(TokenType.FIELD, new FieldParser());
    mPrefixParsers.define("break", new BreakParser());
    // TODO(bob): Rename to just "def" when old def parser is no longer needed.
    mPrefixParsers.define("defmethod", new DefParser());
    mPrefixParsers.define("fn", new FnParser());
    mPrefixParsers.define("nothing", new NothingParser());
    mPrefixParsers.define("return", new ReturnParser());
    mPrefixParsers.define("this", new ThisParser());
    mPrefixParsers.define("unsafecast", new UnsafeCastParser());
    mPrefixParsers.define("using", new UsingParser());
    mPrefixParsers.define("var", new VarParser());

    mInfixParsers.define(TokenType.LEFT_PAREN, new ParenthesisInfixParser());
    mInfixParsers.define(TokenType.NAME, new MessageInfixParser());
    mInfixParsers.define(TokenType.LEFT_BRACKET, new BracketParser());
    mInfixParsers.define("with", new WithParser());
    mInfixParsers.define(TokenType.COMMA, new CommaParser());
    mInfixParsers.define("=", new EqualsParser());

    // Register the parsers for the different keywords.
    // TODO(bob): Eventually these should all go away.
    mPrefixParsers.define("match", new MatchParser());
    mPrefixParsers.define("for", new LoopParser());
    mPrefixParsers.define("while", new LoopParser());

    mPrefixParsers.define("do", new DoParser());
    mPrefixParsers.define("class", new ClassParser());
    mPrefixParsers.define("extend", new ExtendParser());
    mPrefixParsers.define("interface", new InterfaceParser());
    
    mReservedWords.add("->");
    mReservedWords.add("case");
    mReservedWords.add("catch");
    mReservedWords.add("then");    
  }
  
  public void registerParser(String keyword, PrefixParser parser) {
    mPrefixParsers.define(keyword, parser);
  }
  
  public void registerParser(String keyword, InfixParser parser) {
    mInfixParsers.define(keyword, parser);
  }

  public void reserveWord(String name) {
    mReservedWords.add(name);
  }

  public PrefixParser getPrefixParser(Token token) {
    return mPrefixParsers.get(token);
  }

  public InfixParser getInfixParser(Token token) {
    return mInfixParsers.get(token);
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
    if (token.getType() == TokenType.NAME) {
      return mReservedWords.contains(token.getString());
    }
    
    return false;
  }
  
  /**
   * Gets whether or not the name is a "keyword". A keyword is any name that
   * has special meaning to the parser: it's either a reserved word, or it has
   * a prefix or infix parser registered to the name.
   */
  public boolean isKeyword(String name) {
    return mPrefixParsers.isReserved(name) ||
        mInfixParsers.isReserved(name) ||
        mReservedWords.contains(name);
  }
  
  public int getStickiness(Token token) {
    int stickiness = 0;
    
    // A reserved word can't start an infix expression. Prevents us from
    // parsing a keyword as an identifier.
    if (isReserved(token)) return 0;
    
    // If we have a prefix parser for this token's name, then that takes
    // precedence. Prevents us from parsing a keyword as an identifier.
    if ((token.getValue() instanceof String) &&
        mPrefixParsers.isReserved(token.getString())) return 0;

    InfixParser parser = mInfixParsers.get(token);
    if (parser != null) {
      stickiness = parser.getStickiness();
    }
    
    return stickiness;
  }
  
  private final ParserTable<PrefixParser> mPrefixParsers =
      new ParserTable<PrefixParser>();
  private final ParserTable<InfixParser> mInfixParsers =
      new ParserTable<InfixParser>();
  private final Set<String> mReservedWords = new HashSet<String>();
}
