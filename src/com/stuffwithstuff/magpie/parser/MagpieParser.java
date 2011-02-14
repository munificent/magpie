package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
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
          constraint = parseTypeAnnotation();
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
        returnType = parseTypeAnnotation();
      }
      consume(TokenType.RIGHT_PAREN);
    }
    
    return new FunctionType(typeParams, pattern, returnType);
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
    return mGrammar.isKeyword(name);
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

  private final Grammar mGrammar;
  
  // Counts the number of nested expression literals the parser is currently
  // within. Zero means the parser is not inside an expression literal at all
  // (i.e. in regular code). It will be one at the "here" token in "{ here }".
  // Used to determine when an unquote expression is allowed.
  private int mQuoteDepth;
  
  private int mUniqueSymbolId = 0;
}
