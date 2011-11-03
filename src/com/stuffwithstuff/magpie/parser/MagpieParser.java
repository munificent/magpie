package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.SourceReader;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {
  public static MagpieParser create(String text) {
    return create(new StringReader("", text));
  }
  
  public static MagpieParser create(SourceReader reader) {
    return create(reader, new Grammar());
  }
  
  public static MagpieParser create(SourceReader reader, Grammar grammar) {
    TokenReader lexer = new Lexer(reader);
    TokenReader morpher = new Morpher(lexer);
    TokenReader annotator = new Annotator(morpher);
    
    return new MagpieParser(annotator, grammar);
  }
  
  public MagpieParser(TokenReader tokens, Grammar grammar) {
    super(tokens);
    
    mGrammar = grammar;
  }
  
  public Expr parseTopLevelExpression() {
    if (lookAhead(TokenType.EOF)) return null;
    
    Expr expr = parseExpression();
    if (!lookAhead(TokenType.EOF)) consume(TokenType.LINE);
    return expr;
  }
  
  public Expr parseExpression(int precedence) {
    // Top down operator precedence parser based on:
    // http://javascript.crockford.com/tdop/tdop.html
    Token token = consume();
    PrefixParser prefix = mGrammar.getPrefixParser(token);
    
    if (prefix == null) {
      throw new ParseException(String.format(
          "Cannot parse an expression that starts with \"%s\".", token));
    }
    
    Expect.notNull(prefix);
    Expr left = prefix.parse(this, token);
    
    while (precedence < mGrammar.getPrecedence(current())) {
      token = consume();
      
      InfixParser infix = mGrammar.getInfixParser(token);
      left = infix.parse(this, left, token);
    }
    
    return left;
  }
  
  public Expr parseExpression() {
    return parseExpression(0);
  }

  public Expr parseBlock() {
    return parseBlock(true, new String[] { "end" }).getKey();
  }
  
  public Expr parseExpressionOrBlock() {
    return parseExpressionOrBlock("end").getKey();
  }

  public Pair<Expr, Token> parseExpressionOrBlock(String... endTokens) {
    return parseExpressionOrBlock(true, endTokens);
  }

  /**
   * Parses a function type declaration.
   */
  public Pattern parseFunctionType() {
    // Parse the prototype: (foo Foo, bar Bar)
    consume(TokenType.LEFT_PAREN);
    
    // Parse the parameter pattern, if any.
    Pattern pattern = null;
    if (!lookAhead(TokenType.RIGHT_PAREN)) {
      pattern = PatternParser.parse(this);
    } else {
      // No pattern, so expect nothing.
      pattern = Pattern.nothing();
    }

    consume(TokenType.RIGHT_PAREN);
    
    return pattern;
  }

  public Expr groupExpression(TokenType right) {
    PositionSpan span = span();
    if (match(right)) {
      return Expr.nothing(span.end());
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
  
  public boolean inQuote() {
    return mQuoteDepth > 0;
  }
  
  public void pushQuote() {
    mQuoteDepth++;
  }
  
  public void popQuote() {
    mQuoteDepth--;
  }

  @Override
  protected boolean isReserved(String name) {
    return mGrammar.isReserved(name);
  }
  
  private Pair<Expr, Token> parseExpressionOrBlock(boolean parseCatch,
      Object[] endTokens) {
    if (lookAhead(TokenType.LINE)){
      return parseBlock(parseCatch, endTokens);
    } else {
      Expr body = parseExpression();
      return new Pair<Expr, Token>(body, null);
    }
  }
  
  private Pair<Expr, Token> parseBlock(boolean parseCatch,
      Object[] endTokens) {
    consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();
    
    while (true) {
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
    List<MatchCase> catches = new ArrayList<MatchCase>();
    if (parseCatch) {
      while (match("catch")) {
        catches.add(parseCatch(endTokens));
      }
    }

    Expr expr = Expr.sequence(exprs);
    if (catches.size() > 0) {
      expr = Expr.scope(expr, catches);
    }
    
    return new Pair<Expr, Token>(expr, endToken);
  }
  
  private MatchCase parseCatch(Object[] endTokens) {
    Pattern pattern = PatternParser.parse(this);

    consume("then");

    Pair<Expr, Token> body = parseExpressionOrBlock(false, endTokens);

    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) && lookAhead(TokenType.LINE, "catch")) {
      consume();
    }

    return new MatchCase(pattern, body.getKey());
  }

  private final Grammar mGrammar;
  private int mUniqueSymbolId = 0;
  private int mQuoteDepth = 0;
}
