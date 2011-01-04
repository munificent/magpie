package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {  
  public MagpieParser(Lexer lexer, Map<String, ExprParser> parsewords,
      Set<String> keywords) {
    super(lexer);
    
    // Register the parsers for the different keywords.
    // TODO(bob): Eventually these should all go away.
    mParsers.put(TokenType.CLASS, new ClassExprParser());
    mParsers.put(TokenType.EXTEND, new ExtendExprParser());
    mParsers.put(TokenType.FOR, new LoopExprParser());
    mParsers.put(TokenType.INTERFACE, new InterfaceExprParser());
    mParsers.put(TokenType.LET, new LetExprParser());
    mParsers.put(TokenType.MATCH, new MatchExprParser());
    mParsers.put(TokenType.WHILE, new LoopExprParser());
    
    mKeywordParsers = parsewords;
    mKeywords = keywords;
  }
  
  public MagpieParser(Lexer lexer) {
    this(lexer, null, null);
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
  
  public Expr parseExpression() {
    return assignment();
  }

  public Expr parseEndBlock() {
    return parseBlock("end").getKey();
  }

  public Pair<Expr, Token> parseBlock(TokenType... endTokens) {
    return parseBlock(true, null, null, endTokens);
  }

  public Pair<Expr, Token> parseBlock(String keyword, TokenType... endTokens) {
    return parseBlock(true, keyword, null, endTokens);
  }

  public Pair<Expr, Token> parseBlock(String keyword1, String keyword2, TokenType... endTokens) {
    return parseBlock(true, keyword1, keyword2, endTokens);
  }
  
  private Pair<Expr, Token> parseBlock(boolean parseCatch, String keyword1,
      String keyword2, TokenType... endTokens) {
    if (match(TokenType.LINE)){
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (true) {
        // TODO(bob): This keyword stuff is temporary until all keywords are
        // moved into Magpie.
        if (lookAhead(keyword1)) break;
        if (lookAhead(keyword2)) break;
        if (lookAheadAny(endTokens)) break;
        if (lookAhead(TokenType.CATCH)) break;
        
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
        List<MatchCase> catches = new ArrayList<MatchCase>();
        while (match(TokenType.CATCH)) {
          catches.add(parseCatch(keyword1, keyword2, endTokens));
        }
        
        // TODO(bob): This is all pretty hokey.
        if (catches.size() > 0) {
          Expr valueExpr = Expr.name("__err__");
          Expr elseExpr = Expr.message(Expr.name("Runtime"), "throw", valueExpr);
          catchExpr = MatchExprParser.desugarCases(valueExpr, catches, elseExpr);
        }
      }
      
      return new Pair<Expr, Token>(
          Expr.block(exprs, catchExpr), endToken);
    } else {
      Expr body = parseExpression();
      return new Pair<Expr, Token>(body, null);
    }
  }

  private MatchCase parseCatch(String keyword1, String keyword2, TokenType... endTokens) {
    Pattern pattern = MatchExprParser.parsePattern(this);

    consume("then");
    
    Pair<Expr, Token> body = parseBlock(false, keyword1, keyword2, endTokens);
    
    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) &&
        lookAhead(TokenType.LINE, TokenType.CATCH)) {
      consume();
    }
    
    return new MatchCase(pattern, body.getKey());
  }
  
  // fn (a) print "hi"
  public Expr parseFunction() {
    Position position = current().getPosition();
    
    // Parse the static parameters if present.
    FunctionType staticType = null;
    if (lookAhead(TokenType.LEFT_BRACKET)) {
      staticType = parseFunctionType(true);
    }

    // Parse the dynamic parameters if present.
    FunctionType type = null;
    if (lookAhead(TokenType.LEFT_PAREN)) {
      type = parseFunctionType();
    }
    
    // Parse the body.
    Expr expr = parseEndBlock();
    
    position = position.union(last(1).getPosition());
    
    // If neither dynamic nor static parameters were provided, infer a dynamic
    // signature.
    if ((type == null) && (staticType == null)) {
      type = FunctionType.nothingToDynamic();
    }
    
    // Wrap the body in a dynamic function.
    if (type != null) {
      expr = Expr.fn(position, type, expr);
    }
    
    // Wrap it in a static function.
    if (staticType != null) {
      // If the static function is wrapping a dynamic one, we can infer the
      // return type of the static function from it.
      // TODO(bob): Ugly!
      if ((type != null) &&
          (staticType.getReturnType() instanceof MessageExpr) &&
          (((MessageExpr)staticType.getReturnType()).getReceiver() == null) &&
          (((MessageExpr)staticType.getReturnType()).getName().equals("Dynamic"))) {
        staticType = new FunctionType(staticType.getParamNames(),
            staticType.getParamType(),
            Expr.message(null, Name.FAT_ARROW,
                Expr.tuple(type.getParamType(), type.getReturnType())),
                true);
      }
        
      expr = Expr.fn(position, staticType, expr);
    }
    
    return expr;
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
    return parseFunctionType(false);
  }
  
  public FunctionType parseFunctionType(boolean isStatic) {
    TokenType left = TokenType.LEFT_PAREN;
    TokenType right = TokenType.RIGHT_PAREN;
    if (isStatic) {
      left = TokenType.LEFT_BRACKET;
      right = TokenType.RIGHT_BRACKET;
    }
    
    // Parse the prototype: (foo Foo, bar Bar -> Bang)
    consume(left);
    
    // Parse the parameters, if any.
    List<String> paramNames = new ArrayList<String>();
    List<Expr> paramTypes = new ArrayList<Expr>();
    while (!lookAheadAny(TokenType.ARROW, right)){
      paramNames.add(consume(TokenType.NAME).getString());
      
      if (!lookAheadAny(TokenType.ARROW, TokenType.COMMA, right)) {
        paramTypes.add(parseTypeExpression());
      } else {
        paramTypes.add(Expr.name("Dynamic"));
      }
      
      if (!match(TokenType.COMMA)) break;
    }
    
    // Aggregate the parameter types into a single type.
    Expr paramType = null;
    switch (paramTypes.size()) {
    case 0:  paramType = Expr.name("Nothing"); break;
    case 1:  paramType = paramTypes.get(0); break;
    default: paramType = Expr.tuple(paramTypes);
    }
    
    // Parse the return type, if any.
    Expr returnType = null;
    if (match(right)) {
      // No return type, so infer dynamic.
      returnType = Expr.name("Dynamic");
    } else {
      consume(TokenType.ARROW);
      
      if (lookAhead(right)) {
        // An arrow, but no return type, so infer nothing.
        returnType = Expr.name("Nothing");
      } else {
        returnType = parseTypeExpression();
      }
      consume(right);
    }
    
    return new FunctionType(paramNames, paramType, returnType, isStatic);
  }
  
  public String parseFunctionName() {
    return consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
  }
  
  public Expr parseTypeExpression() {
    // Any Magpie expression can be used as a type declaration.
    return operator();
  }

  @Override
  protected boolean isKeyword(String name) {
    return ((mKeywordParsers != null) && mKeywordParsers.containsKey(name)) ||
           ((mKeywords != null) && mKeywords.contains(name));
  }

  private Expr assignment() {
    Expr expr = composite();
    
    if (match(TokenType.EQUALS)) {
      // Parse the value being assigned.
      Expr value = parseExpression();

      Position position = expr.getPosition().union(value.getPosition());
      
      // Translate the left-hand side of the assignment into an appropriate
      // form. This basically avoids the need for explicit L-values.
      // TODO(bob): Need to handle tuples here too.
      if (expr instanceof MessageExpr) {
        // example: point x = 2
        // before:  Msg(      Msg(null, "point"), "x")
        // after:   AssignMsg(Msg(null, "point"), "x", Int(2))
        MessageExpr message = (MessageExpr) expr;
        return Expr.assign(position, message.getReceiver(),
            message.getName(), value);
      } else if (expr instanceof ApplyExpr) {
        // example: array(3) = 4
        // before:  Apply(    Msg(null, "array"),                  Int(3))
        // after:   Apply(Msg(Msg(null, "array"), "assign"), Tuple(Int(3), Int(4)))
        ApplyExpr apply = (ApplyExpr) expr;
        return Expr.message(position, apply.getTarget(), Name.ASSIGN,
            Expr.tuple(apply.getArg(), value));
      } else {
        throw new ParseException("Expression \"" + expr +
        "\" is not a valid target for assignment.");
      }
    }
    
    return expr;
  }

  /**
   * Parses a composite literal: a tuple ("a, b") or a record ("x: 1, y: 2").
   */
  private Expr composite() {
    if (lookAhead(TokenType.FIELD)) {
      Position position = current().getPosition();
      List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
      do {
        String name = consume(TokenType.FIELD).getString();
        Expr value = conjunction();
        fields.add(new Pair<String, Expr>(name, value));
      } while (match(TokenType.COMMA));
      
      return new RecordExpr(position, fields);
    } else {
      List<Expr> fields = new ArrayList<Expr>();
      do {
        fields.add(conjunction());
      } while (match(TokenType.COMMA));
      
      // Only wrap in a tuple if there are multiple fields.
      if (fields.size() == 1) return fields.get(0);
      
      return Expr.tuple(fields);
    }
  }
  
  /**
   * Parses "and" and "or" expressions.
   * @return
   */
  private Expr conjunction() {
    Expr left = operator();
    
    while (matchAny(TokenType.AND, TokenType.OR)) {
      Token conjunction = last(1);
      Expr right = operator();

      if (conjunction.getType() == TokenType.AND) {
        left = Expr.and(left, right);
      } else {
        left = Expr.or(left, right);
      }
    }
    
    return left;
  }
  
  /**
   * Parses a series of operator expressions like "a + b - c".
   */
  private Expr operator() {
    Expr left = message();
    
    while (match(TokenType.OPERATOR)) {
      String op = last(1).getString();
      Expr right = message();

      left = Expr.message(null, op, Expr.tuple(left, right));
    }
    
    return left;
  }
  
  /**
   * Parse a series of message sends, argument applies, and static argument
   * applies. Basically everything in the core syntax that works left-to-right.
   */
  private Expr message() {
    Expr message = primary();
    
    while (true) {
      if (match(TokenType.NAME)) {
        message = Expr.message(last(1).getPosition(), message,
            last(1).getString());
      } else if (match(TokenType.LEFT_BRACKET)) {
        // A static apply (i.e. foo[123]).
        Expr arg;
        if (match(TokenType.RIGHT_BRACKET)) {
          arg = Expr.nothing(Position.surrounding(last(2), last(1)));
        } else {
          arg = parseExpression();
          consume(TokenType.RIGHT_BRACKET);
        }
        message = Expr.apply(message, arg, true);
      } else if (lookAhead(TokenType.LEFT_PAREN)) {
        // A function application like foo(123).
        Expr arg = parenthesizedExpression(BraceType.PAREN);
        message = Expr.apply(message, arg, false);
      } else if (match(TokenType.WITH)) {
        // Parse the parameter list if given.
        FunctionType blockType;
        if (lookAhead(TokenType.LEFT_PAREN)) {
          blockType = parseFunctionType();
        } else {
          // Else just assume a single "it" parameter.
          blockType = new FunctionType(Collections.singletonList(Name.IT),
              Expr.name("Dynamic"), Expr.name("Dynamic"), false);
        }

        // Parse the block and wrap it in a function.
        Expr block = parseEndBlock();
        block = Expr.fn(block.getPosition(), blockType, block);
        
        // Apply it to the previous expression.
        if (message instanceof ApplyExpr) {
          // foo(123) with ...  --> Apply(Msg(foo), Tuple(123, block))
          ApplyExpr apply = (ApplyExpr)message;
          Expr arg = addTupleField(apply.getArg(), block);
          message = Expr.apply(apply.getTarget(), arg, false);
        } else {
          // 123 with ...  --> Apply(Int(123), block)
          message = Expr.apply(message, block, false);
        }
      } else {
        break;
      }
    }
    
    if (message == null) {
      throw new ParseException("Could not parse expression at " +
          current().getPosition());
    }
    
    return message;
  }
  
  /**
   * Parses a primary expression like a literal.
   * @return The parsed expression or null if unsuccessful.
   */
  private Expr primary() {
    if (match(TokenType.BOOL)){
    return Expr.bool(last(1).getPosition(), last(1).getBool());
    } else if (match(TokenType.INT)) {
      return Expr.int_(last(1).getPosition(), last(1).getInt());
    } else if (match(TokenType.STRING)) {
      return Expr.string(last(1).getPosition(), last(1).getString());
    } else if (match(TokenType.THIS)) {
      return Expr.this_(last(1).getPosition());
    } else if (match(TokenType.NOTHING)) {
      return Expr.nothing(last(1).getPosition());
    } else if ((mQuoteDepth > 0) && match(TokenType.BACKTICK)) {
      Position position = last(1).getPosition();
      Expr body;
      if (match(TokenType.NAME)) {
        body = Expr.message(last(1).getPosition(), null, last(1).getString());
      } else if (lookAhead(TokenType.LEFT_BRACE)) {
        body = parenthesizedExpression(BraceType.CURLY);
        body = Expr.quote(body.getPosition(), body);
      } else {
        body = parenthesizedExpression(BraceType.PAREN);
      }
      return new UnquoteExpr(position, body);
    } else if (lookAhead(TokenType.LEFT_PAREN)) {
      return parenthesizedExpression(BraceType.PAREN);
    } else if (lookAhead(TokenType.LEFT_BRACE)) {
      mQuoteDepth++;
      Position position = current().getPosition();
      Expr expr = parenthesizedExpression(BraceType.CURLY);
      position = position.union(last(1).getPosition());
      mQuoteDepth--;
      return Expr.quote(position, expr);
    }
    
    // See if we're at a keyword we know how to parse.
    if ((mKeywordParsers != null) && (current().getType() == TokenType.NAME)) {
      ExprParser parser = mKeywordParsers.get(current().getString());
      if (parser != null) {
        return parser.parse(this);
      }
    }
    ExprParser parser = mParsers.get(current().getType());
    if (parser != null) {
      return parser.parse(this);
    }
    
    // Otherwise fail.
    return null;
  }

  // TODO(bob): Having two ways to create blocks (this, or using a "do"
  // expression) is redundant. Parenthesized blocks are nice because they're
  // part of the core syntax. "do" blocks match the rest of the language and
  // allow catch blocks. If the only reason to have these is for the core
  // syntax, it may be better to just have a %block% special form.
  private Expr parenthesizedExpression(BraceType braceType) {
    TokenType leftBrace = TokenType.LEFT_PAREN;
    TokenType rightBrace = TokenType.RIGHT_PAREN;
    switch (braceType) {
    case SQUARE:
      leftBrace = TokenType.LEFT_BRACKET;
      rightBrace = TokenType.RIGHT_BRACKET;
      break;
    case CURLY:
      leftBrace = TokenType.LEFT_BRACE;
      rightBrace = TokenType.RIGHT_BRACE;
      break;
    }
    
    consume(leftBrace);
    
    // Ignore a leading newline.
    match(TokenType.LINE);
    
    if (match(rightBrace)) {
      return Expr.nothing(Position.surrounding(last(2), last(1)));
    }
    
    List<Expr> exprs = new ArrayList<Expr>();
    while (true) {
      exprs.add(parseExpression());
      if (!match(TokenType.LINE)) break;
      // Allow the closing ) to be on its own line.
      if (lookAhead(rightBrace)) break;
    }
    
    // Allow a newline before the final ).
    match(TokenType.LINE);
    consume(rightBrace);
    
    if (exprs.size() > 1) {
      return Expr.block(exprs);
    } else {
      // Just a single expression, so don't wrap it in a block.
      return exprs.get(0);
    }
  }
  
  private Expr addTupleField(Expr expr, Expr field) {
    if (expr instanceof NothingExpr) {
      return field;
    } else if (expr instanceof TupleExpr) {
      TupleExpr tuple = (TupleExpr)expr;
      List<Expr> fields = new ArrayList<Expr>(tuple.getFields());
      fields.add(field);
      return Expr.tuple(fields);
    } else {
      return Expr.tuple(expr, field);
    }
  }
  
  private static enum BraceType {
    PAREN,
    SQUARE,
    CURLY
  }
  private final Map<TokenType, ExprParser> mParsers =
    new HashMap<TokenType, ExprParser>();
  private final Map<String, ExprParser> mKeywordParsers;
  private final Set<String> mKeywords;
  
  // Counts the number of nested expression literals the parser is currently
  // within. Zero means the parser is not inside an expression literal at all
  // (i.e. in regular code). It will be one at the "here" token in "{ here }".
  // Used to determine when an unquote expression is allowed.
  private int mQuoteDepth;
}
