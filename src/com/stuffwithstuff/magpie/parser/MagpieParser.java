package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {  
  public MagpieParser(Lexer lexer, Grammar grammar) {
    super(lexer);
    
    mGrammar = grammar;
  }
  
  public MagpieParser(Lexer lexer) {
    this(lexer, new Grammar());
  }
  
  public Expr parseTopLevelExpression() {
    if (lookAhead(TokenType.EOF)) return null;
    
    Expr expr = parseExpression();
    if (!lookAhead(TokenType.EOF)) consume(TokenType.LINE);
    return expr;
  }
  
  public Expr parseTypeAnnotation() {
    // Start at just above tuple precedence so that those don't get consumed
    // by the type.
    return parseExpression(21);
  }
  
  public Expr parseExpression(int stickiness) {
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
    
    while (stickiness < mGrammar.getStickiness(current())) {
      token = consume();
      
      InfixParser infix = mGrammar.getInfixParser(token);
      left = infix.parse(this, left, token);
    }
    
    return left;
  }
  
  public Expr parseExpression() {
    return parseExpression(0);
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
  
  /**
   * Parses a name at the current parser position. Unlike a simple
   * consume(TokenType.NAME), which will parse a fully-qualified name
   * consisting of a series of NAME tokens separated by DOTs, like "foo.bar.c".
   * @return A Token that merges the fully-qualified name.
   */
  public Token parseName() {
    return parseName(false);
  }
  
  /**
   * Parses a name at the current parser position. Unlike a simple
   * consume(TokenType.NAME), this will parse a fully-qualified name
   * consisting of a series of NAME tokens separated by DOTs, like "foo.bar.c".
   * @return A Token that merges the fully-qualified name.
   */
  public Token parseName(boolean consumedFirst) {
    String name;
    if (consumedFirst) {
      name = last(1).getString();
    } else {
      name = consume(TokenType.NAME).getString();
    }
    
    PositionSpan span = startBefore();

    while (match(TokenType.DOT)) {
      name += "." + consume(TokenType.NAME).getString();
    }
    
    return new Token(span.end(), TokenType.NAME, name);
  }
  
  public Expr parseFunction() {
    PositionSpan span = startAfter();
    
    // Parse the pattern if present.
    Pattern pattern = null;
    if (lookAheadAny(TokenType.LEFT_PAREN)) {
      pattern = parseFunctionType();
    } else {
      pattern = Pattern.wildcard();
    }
    
    // Parse the body.
    Expr expr = parseEndBlock();
        
    return Expr.fn(span.end(), pattern, expr);
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
    PositionSpan span = startBefore();
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
  
  /**
   * Gets whether or not the name is a "keyword". A keyword is any name that
   * has special meaning to the parser: it's either a reserved word, or it has
   * a prefix or infix parser registered to the name.
   */
  @Override
  protected boolean isReserved(String name) {
    return mGrammar.isReserved(name);
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
      List<MatchCase> catches = new ArrayList<MatchCase>();
      if (parseCatch) {
        while (match("catch")) {
          catches.add(parseCatch(endKeywords, endTokens));
        }
      }

      return new Pair<Expr, Token>(Expr.block(exprs, catches), endToken);
    } else {
      Expr body = parseExpression();
      return new Pair<Expr, Token>(body, null);
    }
  }

  private MatchCase parseCatch(String[] endKeywords, TokenType[] endTokens) {
    Pattern pattern = PatternParser.parse(this);

    consume("->");

    Pair<Expr, Token> body = parseBlock(false, endKeywords, endTokens);

    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) && lookAhead(TokenType.LINE, "catch")) {
      consume();
    }

    return new MatchCase(pattern, body.getKey());
  }

  private final Grammar mGrammar;
  private int mUniqueSymbolId = 0;
}
