package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.SourceReader;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {
  public MagpieParser(String text) {
    this(new StringReader("", text));
  }
  
  public MagpieParser(SourceReader reader) {
    this(new Annotator(new Morpher(new Lexer(reader))));
  }
  
  public MagpieParser(TokenReader tokens) {
    super(tokens);
    mGrammar = new Grammar();
  }
  
  public List<Expr> parseModule() {
    List<Expr> exprs = new ArrayList<Expr>();
    
    while (!lookAhead(TokenType.EOF)) {
      Expr expr = parseStatement();
      exprs.add(expr);
      if (!lookAhead(TokenType.EOF)) consume(TokenType.LINE);
    }
    
    return exprs;
  }
  
  /**
   * Magpie's grammar has two main entrypoints. "Statements" (which aren't
   * true statements since everything is an expression in Magpie) are 
   * "top-level" expressions that appear in a block or variable initializer.
   * These are things like "if" and "var". They cannot, for example, appear as
   * the condition in an "if" expection.
   */
  public Expr parseStatement() {
    if (match(TokenType.BREAK)) return parseBreak();
    if (match(TokenType.DEF)) return parseDef();
    if (match(TokenType.DEFCLASS)) return parseDefclass();
    if (match(TokenType.DO)) return parseDo();
    if (match(TokenType.FOR)) return parseLoop();
    if (match(TokenType.IF)) return parseIf();
    if (match(TokenType.IMPORT)) return parseImport();
    if (match(TokenType.MATCH)) return parseMatch();
    if (match(TokenType.RETURN)) return parseReturn();
    if (match(TokenType.THROW)) return parseThrow();
    if (match(TokenType.VAR)) return parseVar(true);
    if (match(TokenType.VAL)) return parseVar(false);
    if (match(TokenType.WHILE)) return parseLoop();
    
    return parseExpression();
  }
  
  Expr parseExpression() {
    return parsePrecedence(0);
  }

  Expr parsePrecedence(int precedence) {
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
  
  Expr parseBlock() {
    return parseBlock(true, new TokenType[] { TokenType.END }).getKey();
  }
  
  Expr parseExpressionOrBlock() {
    return parseExpressionOrBlock(TokenType.END).getKey();
  }

  private Pair<Expr, Token> parseExpressionOrBlock(TokenType... endTokens) {
    return parseExpressionOrBlock(true, endTokens);
  }

  /**
   * Parses a function type declaration.
   */
  Pattern parseFunctionType() {
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

  public Pair<String, Pattern> parseSignature() {
    // No receiver:        def print(text String)
    // No arg method:      def (this String) reverse()
    // Shared method:      def (Int) parse(text String)
    // Getter:             def (this String) count
    // Method on anything: def (this) debugDump()
    // Value receiver:     def (true) not()
    // Value arg:          def fib(0)
    // Constant receiver:  def (LEFT_PAREN) not()
    // Constant arg:       def string(LEFT_PAREN)
    // Setter:             def (this Person) name = (name String)
    // Setter with arg:    def (this List) at(index Int) = (item)
    // Complex receiver:   def (a Int, b Int) sum()
    // Indexer:            def (this String)[index Int]
    // Index setter:       def (this String)[index Int] = (c Char)

    // Parse the left argument, if any.
    Pattern leftArg;
    if (lookAhead(TokenType.LEFT_PAREN)) {
      leftArg = parsePattern();
    } else {
      leftArg = Pattern.nothing();
    }
    
    // Parse the message.
    String name;
    Pattern rightArg;
    if (matchAny(TokenType.NAME,
                 TokenType.ASTERISK,
                 TokenType.SLASH,
                 TokenType.PERCENT,
                 TokenType.PLUS,
                 TokenType.MINUS,
                 TokenType.LT,
                 TokenType.GT,
                 TokenType.LTE,
                 TokenType.GTE,
                 TokenType.EQEQ,
                 TokenType.NOTEQ)) {
      // Regular named message.
      name = last(1).getString();
      
      // Parse the right argument, if any.
      if (lookAhead(TokenType.LEFT_PAREN)) {
        rightArg = parsePattern();
      } else {
        rightArg = Pattern.nothing();
      }
    } else {
      // No name, so it must be an indexer.
      name = "[]";
      consume(TokenType.LEFT_BRACKET);
      
      if (!match(TokenType.RIGHT_BRACKET)) {
        rightArg = PatternParser.parse(this);
        consume(TokenType.RIGHT_BRACKET);
      } else {
        rightArg = Pattern.nothing();
      }
    }
    
    // Parse the setter's rvalue type, if any.
    Pattern setValue = null;
    if (match(TokenType.EQ)) {
      setValue = parsePattern();
    }

    // Combine into a single multimethod pattern.
    Pattern pattern = Pattern.record(leftArg, rightArg);
    
    if (setValue != null) {
      name = Name.makeAssigner(name);
      pattern = Pattern.record(pattern, setValue);
    }
    
    return new Pair<String, Pattern>(name, pattern);
  }
  
  Expr groupExpression(TokenType right) {
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

  private Expr parseBreak() {
    return Expr.break_(last(1).getPosition());
  }
  
  private Expr allowExpressionAfterBlock(Expr expr) {
    // TODO(bob): Hackish. This is to allow infix expressions, particularly
    // method calls, after a the block bodies of some expressions, like:
    //
    // do
    //    123
    // end shouldEqual(123) // <--
    //
    // Need a more elegant way to handle this.

    // Only if we have a block body. Single-expression bodies shouldn't do this.
    if (!last(1).is(TokenType.END)) return expr;
    
    return parseInfix(expr, 0);
  }
  
  private Expr parseDef() {
    PositionSpan span = span();
    
    // Handle a multimethod definition with no specializations.
    if (lookAhead(TokenType.NAME, TokenType.LINE)) {
      String name = consume().getString();
      String doc = "";
      // If there is a doc comment, the method has a block for it.
      if (match(TokenType.LINE, TokenType.DOC_COMMENT)) {
        doc = last(1).getString();
        consume(TokenType.LINE);
        consume(TokenType.END);
      }
      
      return Expr.method(span.end(), doc, name);
    }
    
    Pair<String, Pattern> signature = parseSignature();
    
    // Parse the doc comment if given.
    String doc = "";
    if (match(TokenType.LINE, TokenType.DOC_COMMENT)) {
      doc = last(1).getString();
    }

    if (!lookAhead(TokenType.LINE)) {
      throw new ParseException(current().getPosition(),
          "A method body must be a block.");
    }
    
    Expr body = parseBlock();
    
    return Expr.method(span.end(), doc, 
        signature.getKey(), signature.getValue(), body);
  }
  
  private Pattern parsePattern() {
    consume(TokenType.LEFT_PAREN);
    if (match(TokenType.RIGHT_PAREN)) return Pattern.nothing();
    
    Pattern pattern = PatternParser.parse(this);
    consume(TokenType.RIGHT_PAREN);
    return pattern;
  }
  
  private Expr parseDefclass() {
    PositionSpan span = span();
    String name = consume(TokenType.NAME).getString();
    
    // Parse the parents, if any.
    List<String> parents = new ArrayList<String>();
    if (match(TokenType.IS)) {
      do {
        parents.add(consume(TokenType.NAME).getString());
      } while (match(TokenType.COMMA));
    }
    
    consume(TokenType.LINE);

    // Parse the doc comment if given.
    String doc = "";
    if (match(TokenType.DOC_COMMENT, TokenType.LINE)) {
      doc = last(2).getString();
    }
    
    Map<String, Field> fields = new HashMap<String, Field>();
    
    // Parse the body.
    while (!match(TokenType.END)) {
      if (match(TokenType.VAR)) parseField(true, fields);
      else if (match(TokenType.VAL)) parseField(false, fields);

      consume(TokenType.LINE);
    }
    
    return Expr.class_(span.end(), doc, name, parents, fields);
  }

  private void parseField(boolean isMutable, Map<String, Field> fields) {
    String name = consume(TokenType.NAME).getString();
    
    // Parse the pattern if there is one.
    Pattern pattern;
    if (lookAhead(TokenType.EQ) || lookAhead(TokenType.LINE)) {
      pattern = Pattern.wildcard();
    } else {
      pattern = PatternParser.parse(this);
    }
    
    // Parse the initializer if there is one.
    Expr initializer;
    if (match(TokenType.EQ)) {
      initializer = parseExpressionOrBlock();
    } else {
      initializer = null;
    }
    
    fields.put(name, new Field(isMutable, initializer, pattern));
  }
  
  private Expr parseDo() {
    Expr body = parseBlock();
    return allowExpressionAfterBlock(Expr.scope(body));
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
    if (match(TokenType.AS)) {
      prefix = consume(TokenType.NAME).getString();
    }
    
    // Parse the declarations, if any.
    List<ImportDeclaration> declarations = new ArrayList<ImportDeclaration>();
    boolean isOnly = false;
    
    if (match(TokenType.WITH)) {
      if (match(TokenType.ONLY)) isOnly = true;
      
      consume(TokenType.LINE);
      
      while (!match(TokenType.END)) {
        // TODO(bob): "excluding".
        
        boolean export = match(TokenType.EXPORT);
        
        String name = consume(TokenType.NAME).getString();
        String rename = null;
        if (match(TokenType.AS)) {
          rename = consume(TokenType.NAME).getString();
        }
        
        consume(TokenType.LINE);
        declarations.add(new ImportDeclaration(export, name, rename));
      }
    }
    return Expr.import_(span.end(), scheme, module, prefix, isOnly, declarations);
  }
  
  private Expr parseMatch() {
    PositionSpan span = span();
    
    // Parse the value.
    Expr value = parseExpression();
    
    // Require a newline between the value and the first case.
    consume(TokenType.LINE);
        
    // Parse the cases.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    while (match(TokenType.CASE)) {
      cases.add(parseCase());
    }
    
    // Parse the else case, if present.
    if (match(TokenType.ELSE)) {
      Expr elseCase = parseExpressionOrBlock();
      cases.add(new MatchCase(elseCase));
    }
    
    consume(TokenType.LINE);
    consume(TokenType.END);
    
    return allowExpressionAfterBlock(Expr.match(span.end(), value, cases));
  }
  
  private MatchCase parseCase() {
    Pattern pattern = PatternParser.parse(this);

    consume(TokenType.THEN);
    
    Pair<Expr, Token> bodyParse = parseExpressionOrBlock(
        TokenType.ELSE, TokenType.END, TokenType.CASE);
    
    // Allow newlines to separate single-line case and else cases.
    if ((bodyParse.getValue() == null) &&
        (lookAhead(TokenType.LINE, TokenType.CASE) ||
         lookAhead(TokenType.LINE, TokenType.ELSE))) {
      consume(TokenType.LINE);
    }
    
    return new MatchCase(pattern, bodyParse.getKey());
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
      if (token.is(TokenType.WHILE)) {
        Expr condition = parseExpression();
        eachLoop.add(Expr.if_(condition,
            Expr.nothing(),
            Expr.break_(condition.getPosition())));
      } else {
        PositionSpan iteratorSpan = span();
        Pattern pattern = PatternParser.parse(this);
        consume(TokenType.IN);
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
      
      if (matchAny(TokenType.WHILE, TokenType.FOR)) {
        token = last(1);
      } else {
        break;
      }
    }
    
    consume(TokenType.DO);
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
  
  private Expr parseIf() {
    PositionSpan span = span();
    
    // Parse the condition.
    Expr condition = parseExpressionOrBlock(TokenType.THEN).getKey();

    // Parse the then body.
    consume(TokenType.THEN);
    Pair<Expr, Token> thenResult = parseExpressionOrBlock(
        TokenType.ELSE, TokenType.END);
    Expr thenExpr = thenResult.getKey();
    Token endToken = thenResult.getValue();
    
    // Don't try to parse "else" if we got an explicit "end" for the "then"
    // block.
    boolean consumedEnd = (endToken != null) && endToken.is(TokenType.END);

    // See if we have an "else" keyword and parse the else arm.
    Expr elseExpr;
    if (!consumedEnd && match(TokenType.ELSE)) {
      elseExpr = parseExpressionOrBlock();
    } else {
      elseExpr = Expr.nothing();
    }

    // Desugar to a match.
    // TODO(bob): Should do this in a later pass.
    Expr truthyCondition = Expr.call(condition.getPosition(), condition,
        Name.IS_TRUE);
    List<MatchCase> cases = new ArrayList<MatchCase>();
    cases.add(new MatchCase(Pattern.value(Expr.bool(true)), thenExpr));
    cases.add(new MatchCase(elseExpr));
    
    Expr matchExpr = Expr.match(span.end(), truthyCondition, cases);
    return allowExpressionAfterBlock(matchExpr);
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
    consume(TokenType.EQ);
    Expr value = parseExpressionOrBlock();
    
    return Expr.var(span.end(), isMutable, pattern, value);
  }
  
  private Pair<Expr, Token> parseExpressionOrBlock(boolean parseCatch,
      TokenType[] endTokens) {
    if (lookAhead(TokenType.LINE)){
      return parseBlock(parseCatch, endTokens);
    } else {
      Expr body = parseStatement();
      return new Pair<Expr, Token>(body, null);
    }
  }
  
  private Pair<Expr, Token> parseBlock(boolean parseCatch,
      TokenType[] endTokens) {
    consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();
    
    while (true) {
      if ((endTokens != null) && lookAheadAny(endTokens)) break;
      if (lookAhead(TokenType.CATCH)) break;
      
      exprs.add(parseStatement());
      consume(TokenType.LINE);
    }
    
    Token endToken = current();
    
    // If the block ends with 'end', then we want to consume that token,
    // otherwise we want to leave it unconsumed to be consistent with the
    // single-expression block case.
    if (endToken.is(TokenType.END)) consume();
    
    // Parse any catch clauses.
    List<MatchCase> catches = new ArrayList<MatchCase>();
    if (parseCatch) {
      while (match(TokenType.CATCH)) {
        catches.add(parseCatch(endTokens));
      }
    }

    Expr expr = Expr.sequence(exprs);
    if (catches.size() > 0) {
      expr = Expr.scope(expr, catches);
    }
    
    return new Pair<Expr, Token>(expr, endToken);
  }
  
  private MatchCase parseCatch(TokenType[] endTokens) {
    Pattern pattern = PatternParser.parse(this);

    consume(TokenType.THEN);

    Pair<Expr, Token> body = parseExpressionOrBlock(false, endTokens);

    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) && lookAhead(TokenType.LINE, TokenType.CATCH)) {
      consume();
    }

    return new MatchCase(pattern, body.getKey());
  }

  private final Grammar mGrammar;
  private int mUniqueSymbolId = 0;
  private int mQuoteDepth = 0;
}
