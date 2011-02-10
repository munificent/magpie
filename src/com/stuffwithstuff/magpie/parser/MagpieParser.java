package com.stuffwithstuff.magpie.parser;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {  
  public MagpieParser(Lexer lexer,
      Map<String, PrefixParser> prefixParsers,
      Map<String, InfixParser> infixParsers,
      Set<String> reservedWords) {
    super(lexer);
    
    // Register the built-in parsers.
    mPrefixParsers.define(TokenType.BOOL, new LiteralParser());
    mPrefixParsers.define(TokenType.INT, new LiteralParser());
    mPrefixParsers.define(TokenType.STRING, new LiteralParser());
    mPrefixParsers.define(TokenType.LEFT_PAREN, new ParenthesisPrefixParser());
    mPrefixParsers.define(TokenType.LEFT_BRACE, new BraceParser());
    mPrefixParsers.define(TokenType.BACKTICK, new BacktickParser());
    mPrefixParsers.define(TokenType.NAME, new MessagePrefixParser());
    mPrefixParsers.define(TokenType.FIELD, new FieldParser());
    mPrefixParsers.define("fn", new FnParser());
    mPrefixParsers.define("nothing", new NothingParser());
    mPrefixParsers.define("this", new ThisParser());

    mInfixParsers.define(TokenType.LEFT_PAREN, new ParenthesisInfixParser());
    mInfixParsers.define(TokenType.NAME, new MessageInfixParser());
    mInfixParsers.define(TokenType.LEFT_BRACKET, new BracketParser());
    mInfixParsers.define(TokenType.OPERATOR, new OperatorParser());
    mInfixParsers.define("with", new WithParser());
    mInfixParsers.define(TokenType.COMMA, new CommaParser());
    mInfixParsers.define(TokenType.EQUALS, new EqualsParser());

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
    
    // Register the user-defined parsers.
    if (prefixParsers != null) {
      for (Entry<String, PrefixParser> parser : prefixParsers.entrySet()) {
        mPrefixParsers.define(parser.getKey(), parser.getValue());
      }
    }

    if (infixParsers != null) {
      for (Entry<String, InfixParser> parser : infixParsers.entrySet()) {
        mInfixParsers.define(parser.getKey(), parser.getValue());
      }
    }

    // Register the user-defined reserved words.
    if (reservedWords != null) {
      mReservedWords.addAll(reservedWords);
    }
  }
  
  public MagpieParser(Lexer lexer) {
    this(lexer, null, null, null);
  }
  
  public List<Expr> parse() {
    // Parse the entire file.
    List<Expr> expressions = new ArrayList<Expr>();
    do {
      expressions.add(parseExpression());
      
      // Allow files with no trailing newline.
      if (match(TokenType.EOF)) break;
      
      consume(TokenType.LINE);
    } while (!match(TokenType.EOF));

    return expressions;
  }
  
  public Expr parseExpression(int stickiness) {
    // Top down operator precedence parser based on:
    // http://javascript.crockford.com/tdop/tdop.html
    Token token = consume();
    PrefixParser prefix = mPrefixParsers.get(token);
    Expect.notNull(prefix);
    Expr left = prefix.parse(this, token);
    
    while (stickiness < getStickiness()) {
      token = consume();
      
      InfixParser infix = mInfixParsers.get(token);
      left = infix.parse(this, left, token);
    }
    
    return left;
  }
  
  public Expr parseExpression() {
    return parseExpression(0);
  }
  
  // TODO(bob): Hackish. Do we need this?
  public Expr parseOperator() {
    return parseExpression(80);
  }

  public Expr parseEndBlock() {
    return parseBlock("end").getKey();
  }

  public Pair<Expr, Token> parseBlock(TokenType... endTokens) {
    return parseBlock(true, null, endTokens);
  }

  public Pair<Expr, Token> parseBlock(String keyword, TokenType[] endTokens) {
    return parseBlock(true, new String[] { keyword }, endTokens);
  }

  public Pair<Expr, Token> parseBlock(String... endTokens) {
    return parseBlock(true, endTokens, null);
  }
  
  public Expr parseFunction() {
    Position position = current().getPosition();
    
    // Parse the type signature if present.
    FunctionType type = null;
    if (lookAheadAny(TokenType.LEFT_PAREN, TokenType.LEFT_BRACKET)) {
      type = parseFunctionType();
    }
    
    // Parse the body.
    Expr expr = parseEndBlock();
    
    position = position.union(last(1).getPosition());
    
    // If neither dynamic nor type parameters were provided, infer a dynamic
    // signature.
    if (type == null) {
      type = FunctionType.nothingToDynamic();
    }
    
    return Expr.fn(position, type, expr);
  }

  /**
   * Parses a function type declaration. Valid examples
   * include:
   * (->)           // takes nothing, returns nothing
   * ()             // takes nothing, returns dynamic
   * (a)            // takes a single dynamic, returns dynamic
   * (a ->)         // takes a single dynamic, returns nothing
   * (a Int -> Int) // takes and returns an int
   * 
   * @return The parsed function type.
   */
  public FunctionType parseFunctionType() {
    // Parse the type parameters, if any.
    List<Pair<String, Expr>> typeParams = new ArrayList<Pair<String, Expr>>();
    if (match(TokenType.LEFT_BRACKET)) {
      do {
        String name = consume(TokenType.NAME).getString();
        
        // Infer "Any" if no constraint is given.
        Expr constraint;
        if (lookAheadAny(TokenType.COMMA, TokenType.RIGHT_BRACKET)) {
          constraint = Expr.name("Any");
        } else {
          constraint = TypeParser.parse(this);
        }
        typeParams.add(new Pair<String, Expr>(name, constraint));
      } while (match(TokenType.COMMA));
      
      consume(TokenType.RIGHT_BRACKET);
    }
    
    // Parse the prototype: (foo Foo, bar Bar -> Bang)
    consume(TokenType.LEFT_PAREN);
    
    // Parse the parameter pattern, if any.
    Pattern pattern = null;
    if (!lookAhead("->") && !lookAhead(TokenType.RIGHT_PAREN)) {
      pattern = PatternParser.parse(this);
    } else {
      // No pattern, so expect nothing.
      pattern = new ValuePattern(Expr.nothing());
    }

    // Parse the return type, if any.
    Expr returnType = null;
    if (match(TokenType.RIGHT_PAREN)) {
      // No return type, so infer dynamic.
      returnType = Expr.name("Dynamic");
    } else {
      consume("->");
      
      // Ignore a newline after the arrow.
      match(TokenType.LINE);
      
      if (lookAhead(TokenType.RIGHT_PAREN)) {
        // An arrow, but no return type, so infer nothing.
        returnType = Expr.name("Nothing");
      } else {
        returnType = TypeParser.parse(this);
      }
      consume(TokenType.RIGHT_PAREN);
    }
    
    return new FunctionType(typeParams, pattern, returnType);
  }
  
  public String parseFunctionName() {
    return consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
  }
  
  public boolean inQuotation() {
    return mQuoteDepth > 0;
  }
  
  public void pushQuote() {
    mQuoteDepth++;
  }
  
  public void popQuote() {
    mQuoteDepth--;
  }

  public Expr groupExpression(TokenType right) {
    if (match(right)) {
      return Expr.nothing(Position.surrounding(last(2), last(1)));
    }
    
    Expr expr = parseExpression();
    
    // Allow a newline before the final ).
    match(TokenType.LINE);
    consume(right);
    
    return expr;
  }
  
  public String generateName() {
    // Include a space in the name to avoid colliding with any user-defined
    // names.
    return "gen " + (++mUniqueSymbolId);
  }
  
  /**
   * Gets whether or not the name is a "keyword". A keyword is any name that
   * has special meaning to the parser: it's either a reserved word, or it has
   * a prefix or infix parser registered to the name.
   */
  @Override
  protected boolean isKeyword(String name) {
    return mPrefixParsers.isReserved(name) ||
        mInfixParsers.isReserved(name) ||
        mReservedWords.contains(name);
  }
  
  private int getStickiness() {
    int stickiness = 0;
    
    // A reserved word can't start an infix expression. Prevents us from
    // parsing a keyword as an identifier.
    if (isReserved(current())) return 0;
    
    // If we have a prefix parser for this token's name, then that takes
    // precedence. Prevents us from parsing a keyword as an identifier.
    if ((current().getValue() instanceof String) &&
        mPrefixParsers.isReserved(current().getString())) return 0;

    InfixParser parser = mInfixParsers.get(current());
    if (parser != null) {
      stickiness = parser.getStickiness();
    }
    
    return stickiness;
  }

  /**
   * Gets whether or not this token is a reserved word. Reserved words like
   * "else" and "then" are claimed for special use by mixfix parsers, so can't
   * be parsed on their own.
   * 
   * @param   token  The token to test
   * @return         True if the token is a reserved name token.
   */
  private boolean isReserved(Token token) {
    if ((token.getType() == TokenType.NAME) ||
        (token.getType() == TokenType.OPERATOR)) {
      return mReservedWords.contains(token.getString());
    }
    
    return false;
  }
  
  private Pair<Expr, Token> parseBlock(boolean parseCatch,
      String[] endKeywords, TokenType[] endTokens) {
    if (match(TokenType.LINE)){
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (true) {
        // TODO(bob): This keyword stuff is temporary until all keywords are
        // moved into Magpie.
        if ((endKeywords != null) && lookAheadAny(endKeywords)) break;
        if ((endTokens != null) && lookAheadAny(endTokens)) break;
        if (lookAhead("catch")) break;
        
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      }
      
      Token endToken = current();
      
      // If the block ends with 'end', then we want to consume that token,
      // otherwise we want to leave it unconsumed to be consistent with the
      // single-expression block case.
      if (endToken.isKeyword("end")) {
        consume();
      }
      
      // Parse any catch clauses.
      Expr catchExpr = null;
      if (parseCatch) {
        Position position = current().getPosition();
        List<MatchCase> catches = new ArrayList<MatchCase>();
        while (match("catch")) {
          catches.add(parseCatch(endKeywords, endTokens));
        }
        
        // TODO(bob): This is kind of hokey.
        if (catches.size() > 0) {
          Expr valueExpr = Expr.name("__err__");
          Expr elseExpr = Expr.message(valueExpr.getPosition(),
              Expr.name("Runtime"), "throw", valueExpr);
          catches.add(new MatchCase(new VariablePattern("_", null), elseExpr));
          
          position = position.union(last(1).getPosition());
          catchExpr = Expr.match(position, valueExpr, catches);
        }
      }
      
      return new Pair<Expr, Token>(
          Expr.block(exprs, catchExpr), endToken);
    } else {
      Expr body = parseExpression();
      return new Pair<Expr, Token>(body, null);
    }
  }

  private MatchCase parseCatch(String[] endKeywords, TokenType[] endTokens) {
    Pattern pattern = PatternParser.parse(this);

    consume("then");
    
    Pair<Expr, Token> body = parseBlock(false, endKeywords, endTokens);
    
    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) &&
        lookAhead(TokenType.LINE, "catch")) {
      consume();
    }
    
    return new MatchCase(pattern, body.getKey());
  }

  private final ParserTable<PrefixParser> mPrefixParsers =
      new ParserTable<PrefixParser>();
  private final ParserTable<InfixParser> mInfixParsers =
      new ParserTable<InfixParser>();
  private final Set<String> mReservedWords = new HashSet<String>();
  
  // Counts the number of nested expression literals the parser is currently
  // within. Zero means the parser is not inside an expression literal at all
  // (i.e. in regular code). It will be one at the "here" token in "{ here }".
  // Used to determine when an unquote expression is allowed.
  private int mQuoteDepth;
  
  private int mUniqueSymbolId = 0;
}
