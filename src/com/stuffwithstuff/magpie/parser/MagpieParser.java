package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.SourceReader;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
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
    
    Expr expr = parseStatement();
    if (!lookAhead(TokenType.EOF)) consume(TokenType.LINE);
    return expr;
  }
  
  /**
   * Magpie's grammar has two main entrypoints. "Statements" (which aren't
   * true statements since everything is an expression in Magpie) are 
   * "top-level" expressions that appear in a block or variable initializer.
   * These are things like "if" and "var". They cannot, for example, appear as
   * the condition in an "if" expection.
   */
  public Expr parseStatement() {
    if (match("break")) return parseBreak();
    if (match("do")) return parseDo();
    if (match("for")) return parseLoop();
    if (match("import")) return parseImport();
    if (match("return")) return parseReturn();
    if (match("throw")) return parseThrow();
    if (match("var")) return parseVar(true);
    if (match("val")) return parseVar(false);
    if (match("while")) return parseLoop();
    
    return parseExpression();
  }
  
  public Expr parseExpression() {
    return parsePrecedence(0);
  }

  public Expr parsePrecedence(int precedence) {
    // Top down operator precedence parser based on:
    // http://javascript.crockford.com/tdop/tdop.html
    Token token = consume();
    PrefixParser prefix = mGrammar.getPrefixParser(token);
    
    if (prefix == null) {
      throw new ParseException(token.getPosition(), String.format(
          "Cannot parse an expression that starts with \"%s\".", token));
    }
    
    Expect.notNull(prefix);
    Expr left = prefix.parse(this, token);
    
    return parseInfix(left, precedence);
  }

  private Expr parseInfix(Expr left, int precedence) {
    while (precedence < mGrammar.getPrecedence(current())) {
      Token token = consume();
      
      InfixParser infix = mGrammar.getInfixParser(token);
      left = infix.parse(this, left, token);
    }
    
    return left;
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
  
  private Expr parseBreak() {
    return Expr.break_(last(1).getPosition());
  }
  
  private Expr parseDo() {
    Expr body = parseBlock();
    Expr result = Expr.scope(body);
    
    // TODO(bob): Hackish. This is to allow infix expressions, particularly
    // method calls, after a "do" block, like:
    //
    // do
    //    123
    // end shouldEqual(123) // <--
    //
    // Need a more elegant way to handle this.
    return parseInfix(result, 0);
  }
  
  private Expr parseImport() {
    PositionSpan span = span();
    
    String scheme = null;
    if (match(TokenType.FIELD)) {
      scheme = last(1).getString();
    }
    
    // Parse the module name.
    String module = consume(TokenType.NAME).getString();
    
    // Parse the prefix, if any.
    String prefix = null;
    if (match("as")) {
      prefix = consume(TokenType.NAME).getString();
    }
    
    // Parse the declarations, if any.
    List<ImportDeclaration> declarations = new ArrayList<ImportDeclaration>();
    boolean isOnly = false;
    
    if (match("with")) {
      if (match("only")) isOnly = true;
      
      consume(TokenType.LINE);
      
      while (!match("end")) {
        // TODO(bob): "excluding".
        
        boolean export = match("export");
        
        String name = consume(TokenType.NAME).getString();
        String rename = null;
        if (match("as")) {
          rename = consume(TokenType.NAME).getString();
        }
        
        consume(TokenType.LINE);
        declarations.add(new ImportDeclaration(export, name, rename));
      }
    }
    return Expr.import_(span.end(), scheme, module, prefix, isOnly, declarations);
  }
    
  /**
   * Parse a "while" or "for" loop.
   */
  private Expr parseLoop() {
    Token token = last(1);
    // "while" and "for" loop.
    PositionSpan span = span();
    
    // TODO(bob): Should do this desugaring in a later AST->IR transform. The
    // AST should be closer to a straight parse.
    // A loop is desugared from this:
    //
    //   while bar
    //   for a in foo do
    //       print(a)
    //   end
    //
    // To:
    //
    //   do
    //       // beforeLoop:
    //       var __a_gen = foo iterate()
    //       // end beforeLoop
    //       loop
    //           // eachLoop:
    //           if bar then nothing else break
    //           if __a_gen next() then nothing else break
    //           var a = __a_gen current
    //           // end eachLoop
    //           // body:
    //           print(a)
    //       end
    //   end
    
    List<Expr> beforeLoop = new ArrayList<Expr>();
    List<Expr> eachLoop = new ArrayList<Expr>();
    
    while (true) {
      if (token.isKeyword("while")) {
        Expr condition = parseExpression();
        eachLoop.add(Expr.if_(condition,
            Expr.nothing(),
            Expr.break_(condition.getPosition())));
      } else {
        PositionSpan iteratorSpan = span();
        Pattern pattern = PatternParser.parse(this);
        consume("in");
        Expr generator = parseExpression();
        Position position = iteratorSpan.end();
        
        // Initialize the iterator before the loop.
        String iteratorVar = generateName();
        beforeLoop.add(Expr.var(position, false, iteratorVar,
            Expr.call(position, generator, Name.ITERATE,
                Expr.nothing(position))));
        
        // Each iteration, advance the iterator and break if done.
        eachLoop.add(Expr.if_(
            Expr.call(position, Expr.name(iteratorVar), Name.NEXT, Expr.nothing(position)),
            Expr.nothing(),
            Expr.break_(position)));
        
        // If not done, create the loop variable.
        eachLoop.add(Expr.var(position, false, pattern,
            Expr.call(position, Expr.name(position, iteratorVar), Name.CURRENT)));
      }
      match(TokenType.LINE); // Optional line after a clause.
      
      if (match("while") || match("for")) {
        token = last(1);
      } else {
        break;
      }
    }
    
    consume("do");
    Expr body = parseExpressionOrBlock();

    // Build the loop body.
    List<Expr> loopBlock = new ArrayList<Expr>();
    for (Expr expr : eachLoop) loopBlock.add(expr);

    // Then execute the main body.
    loopBlock.add(body);
    Expr loopBody = Expr.sequence(loopBlock);
    
    // Add the iterators outside of the loop.
    List<Expr> outerBlock = new ArrayList<Expr>();
    for (Expr expr : beforeLoop) outerBlock.add(expr);

    // Add the main loop.
    outerBlock.add(Expr.loop(span.end(), loopBody));

    // Wrap the iterators in their own scope.
    return Expr.scope(Expr.sequence(outerBlock));
  }
  
  private Expr parseReturn() {
    PositionSpan span = span();
    Expr value;
    if (lookAheadAny(TokenType.LINE, TokenType.RIGHT_PAREN,
        TokenType.RIGHT_BRACKET, TokenType.RIGHT_BRACE)) {
      // A return with nothing after it implicitly returns nothing.
      value = Expr.nothing(last(1).getPosition());
    } else {
      value = parseExpression();
    }
    
    return Expr.return_(span.end(), value);
  }
  
  private Expr parseThrow() {
    PositionSpan span = span();
    Expr value = parseExpressionOrBlock();
    return Expr.throw_(span.end(), value);
  }
  
  private Expr parseVar(boolean isMutable) {
    PositionSpan span = span();
    Pattern pattern = PatternParser.parse(this);
    consume(TokenType.EQUALS);
    Expr value = parseExpressionOrBlock();
    
    return Expr.var(span.end(), isMutable, pattern, value);
  }
  
  private Pair<Expr, Token> parseExpressionOrBlock(boolean parseCatch,
      Object[] endTokens) {
    if (lookAhead(TokenType.LINE)){
      return parseBlock(parseCatch, endTokens);
    } else {
      Expr body = parseStatement();
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
      
      exprs.add(parseStatement());
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
