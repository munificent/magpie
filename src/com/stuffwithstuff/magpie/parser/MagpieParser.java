package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;
import com.stuffwithstuff.magpie.util.Ref;

public class MagpieParser extends Parser {  
  public static enum BlockOptions {
    CONSUME_END,
    CONSUME_LINE_AFTER_EXPRESSION
  }

  public MagpieParser(Lexer lexer) {
    super(lexer);
    
    // Register the parsers for the different keywords.
    mParsers.put(TokenType.BREAK, new BreakExprParser());
    mParsers.put(TokenType.CLASS, new ClassExprParser());
    mParsers.put(TokenType.DEF, new DefineExprParser());
    mParsers.put(TokenType.DO, new DoExprParser());
    mParsers.put(TokenType.EXTEND, new ExtendExprParser());
    mParsers.put(TokenType.FN, new FnExprParser());
    mParsers.put(TokenType.FOR, new LoopExprParser());
    mParsers.put(TokenType.GET, new GetExprParser());
    mParsers.put(TokenType.IF, new ConditionalExprParser());
    mParsers.put(TokenType.INTERFACE, new InterfaceExprParser());
    mParsers.put(TokenType.LET, new ConditionalExprParser());
    mParsers.put(TokenType.MATCH, new MatchExprParser());
    mParsers.put(TokenType.RETURN, new ReturnExprParser());
    mParsers.put(TokenType.SHARED, new DefineExprParser());
    mParsers.put(TokenType.TYPEOF, new TypeofExprParser());
    mParsers.put(TokenType.UNSAFECAST, new UnsafeCastExprParser());
    mParsers.put(TokenType.VAR, new VariableExprParser());
    mParsers.put(TokenType.WHILE, new LoopExprParser());
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

  public Expr parseBlock() {
    return parseBlock(EnumSet.of(BlockOptions.CONSUME_END));
  }

  public Expr parseBlock(EnumSet<BlockOptions> options,
      TokenType... endTokenTypes) {
    return parseBlock(new Ref<Boolean>(), options, endTokenTypes);
  }
  
  public Expr parseBlock(Ref<Boolean> consumedEnd, EnumSet<BlockOptions> options,
      TokenType... endTokenTypes) {
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (true) {
        if (options.contains(BlockOptions.CONSUME_END) &&
            lookAhead(TokenType.END)) break;
        if (lookAheadAny(endTokenTypes)) break;
        if (lookAhead(TokenType.CATCH)) break;
        
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      }
      
      List<MatchCase> catches = new ArrayList<MatchCase>();
      while (match(TokenType.CATCH)) {
        catches.add(parseCatch());
      }
      
      // If the block ends with 'end', then we want to consume that token,
      // otherwise we want to leave it unconsumed to be consistent with the
      // single-expression block case.
      if (options.contains(BlockOptions.CONSUME_END) && match(TokenType.END)) {
        consumedEnd.set(true);
      } else {
        consumedEnd.set(false);
      }
      
      // TODO(bob): This is all pretty hokey.
      Expr catchExpr = null;
      if (catches.size() > 0) {
        Expr valueExpr = Expr.name("__err__");
        Expr elseExpr = Expr.message(Expr.name("Runtime"), "throw", valueExpr);
        catchExpr = MatchExprParser.desugarCases(valueExpr, catches, elseExpr);
      }
      
      position = position.union(last(1).getPosition());
      return new BlockExpr(position, exprs, catchExpr);
    } else {
      Expr body = parseExpression();
      if (options.contains(BlockOptions.CONSUME_LINE_AFTER_EXPRESSION)) {
        consume(TokenType.LINE);
      }
      consumedEnd.set(false);
      return body;
    }
  }
  
  private MatchCase parseCatch() {
    
    String name = MatchExprParser.parseBinding(this);
    Pattern pattern = MatchExprParser.parsePattern(this);

    // Infer 'it' for the matched value if no name is provided.
    if (name == null) name = "it";
    
    consume(TokenType.THEN);
    
    Expr body;
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (!lookAheadAny(TokenType.CATCH, TokenType.END)) {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      }
      
      position = position.union(last(1).getPosition());
      body = new BlockExpr(position, exprs);
    } else {
      body = parseExpression();
      consume(TokenType.LINE);
    }

    return new MatchCase(name, pattern, body);
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
    Expr expr = parseBlock();
    
    position = position.union(last(1).getPosition());
    
    // If neither dynamic nor static parameters were provided, infer a dynamic
    // signature.
    if ((type == null) && (staticType == null)) {
      type = FunctionType.nothingToDynamic();
    }
    
    // Wrap the body in a dynamic function.
    if (type != null) {
      expr = new FnExpr(position, type, expr, false);
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
            Expr.message(type.getParamType(), "=>", type.getReturnType()));
      }
        
      expr = new FnExpr(position, staticType, expr, true);
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
    default: paramType = new TupleExpr(paramTypes);
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
    
    return new FunctionType(paramNames, paramType, returnType);
  }
  
  public String parseFunctionName() {
    return consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
  }
  
  public Expr parseTypeExpression() {
    // Any Magpie expression can be used as a type declaration.
    return operator();
  }
  
  private Expr assignment() {
    Expr expr = tuple();
    
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
        return new AssignExpr(position, message.getReceiver(),
            message.getName(), value);
      } else if (expr instanceof ApplyExpr) {
        // example: array(3) = 4
        // before:  Apply(    Msg(null, "array"),                  Int(3))
        // after:   Apply(Msg(Msg(null, "array"), "assign"), Tuple(Int(3), Int(4)))
        ApplyExpr apply = (ApplyExpr) expr;
        return new ApplyExpr(new MessageExpr(position, apply.getTarget(),
            Identifiers.ASSIGN),
            Expr.tuple(apply.getArg(), value), false);
      } else {
        throw new ParseException("Expression \"" + expr +
        "\" is not a valid target for assignment.");
      }
    }
    
    return expr;
  }

  /**
   * Parses a tuple expression like "a, b, c".
   */
  private Expr tuple() {
    List<Expr> fields = parseCommaList();
        
    // Only wrap in a tuple if there are multiple fields.
    if (fields.size() == 1) return fields.get(0);
    
    return new TupleExpr(fields);
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

      Position position = left.getPosition().union(right.getPosition());

      if (conjunction.getType() == TokenType.AND) {
        left = new AndExpr(position, left, right);
      } else {
        left = new OrExpr(position, left, right);
      }
    }
    
    return left;
  }
  
  /**
   * Parses a series of operator expressions like "a + b - c".
   */
  private Expr operator() {
    Expr left = blockArgument();
    
    while (match(TokenType.OPERATOR)) {
      String op = last(1).getString();
      Expr right = blockArgument();

      left = Expr.message(left.getPosition().union(right.getPosition()),
          left, op, right);
    }
    
    return left;
  }
  
  /**
   * Parse series of block arguments (i.e. a "with" block).
   */
  private Expr blockArgument() {
    Expr expr = message();
    
    while(match(TokenType.WITH)) {
      // Parse the parameter list if given.
      FunctionType blockType;
      if (lookAhead(TokenType.LEFT_PAREN)) {
        blockType = parseFunctionType();
      } else {
        // Else just assume a single "it" parameter.
        blockType = new FunctionType(Collections.singletonList(Identifiers.IT),
            Expr.name("Dynamic"), Expr.name("Dynamic"));
      }

      // Parse the block and wrap it in a function.
      Expr block = parseBlock();
      block = new FnExpr(block.getPosition(), blockType, block, false);
      
      // Apply it to the previous expression.
      if (expr instanceof ApplyExpr) {
        // foo(123) with ...  --> Apply(Msg(foo), Tuple(123, block))
        ApplyExpr apply = (ApplyExpr)expr;
        Expr arg = addTupleField(apply.getArg(), block);
        expr = new ApplyExpr(apply.getTarget(), arg, false);
      } else {
        // 123 with ...  --> Apply(Int(123), block)
        expr = new ApplyExpr(expr, block, false);
      }
    }
    
    return expr;
  }
  
  /**
   * Parse a series of message sends, argument applies, and static argument
   * applies. Basically everything in the core syntax that works left-to-right.
   */
  private Expr message() {
    Expr message = primary();
    
    while (true) {
      if (match(TokenType.NAME)) {
        message = new MessageExpr(last(1).getPosition(), message,
            last(1).getString());
      } else if (match(TokenType.LEFT_BRACKET)) {
        // A static apply (i.e. foo[123]).
        Expr arg;
        if (match(TokenType.RIGHT_BRACKET)) {
          arg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else {
          arg = parseExpression();
          consume(TokenType.RIGHT_BRACKET);
        }
        message = new ApplyExpr(message, arg, true);
      } else if (match(TokenType.LEFT_PAREN)) {
        // A function application like foo(123).
        Expr arg;
        if (match(TokenType.RIGHT_PAREN)) {
          arg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else {
          arg = parseExpression();
          consume(TokenType.RIGHT_PAREN);
        }
        message = new ApplyExpr(message, arg, false);
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
    return new BoolExpr(last(1));
    } else if (match(TokenType.INT)) {
      return new IntExpr(last(1));
    } else if (match(TokenType.STRING)) {
      return new StringExpr(last(1));
    } else if (match(TokenType.THIS)) {
      return new ThisExpr(last(1).getPosition());
    } else if (match(TokenType.NOTHING)) {
      return new NothingExpr(last(1).getPosition());
    } else if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
      return new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
    } else if (match(TokenType.LEFT_PAREN)) {
      Expr expr = parseExpression();
      consume(TokenType.RIGHT_PAREN);
      return expr;
    } else if (match(TokenType.LEFT_BRACE)) {
      Position position = last(1).getPosition();
      Expr expr = parseExpression();
      consume(TokenType.RIGHT_BRACE);
      position = position.union(last(1).getPosition());
      return new ExpressionExpr(position, expr);
    } else if (lookAhead(TokenType.NAME, TokenType.COLON)) {
      return parseObjectLiteral();
    }
    
    // See if we're at a keyword we know how to parse.
    ExprParser parser = mParsers.get(current().getType());
    if (parser != null) {
      return parser.parse(this);
    }
    
    // Otherwise fail.
    return null;
  }
  
  /**
   * Parses an object literal like "x: 1 y: 2"
   */
  private Expr parseObjectLiteral() {
    Position position = current().getPosition();
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    while (match(TokenType.NAME, TokenType.COLON)) {
      String name = last(2).getString();
      Expr value = parseExpression();
      fields.add(new Pair<String, Expr>(name, value));
    }
    
    return new ObjectExpr(position, fields);
  }
  
  private List<Expr> parseCommaList() {
    List<Expr> exprs = new ArrayList<Expr>();
    
    do {
      exprs.add(conjunction());
    } while (match(TokenType.COMMA));
    
    return exprs;
  }

  private Expr addTupleField(Expr expr, Expr field) {
    if (expr instanceof NothingExpr) {
      return field;
    } else if (expr instanceof TupleExpr) {
      TupleExpr tuple = (TupleExpr)expr;
      List<Expr> fields = new ArrayList<Expr>(tuple.getFields());
      fields.add(field);
      return new TupleExpr(fields);
    } else {
      return Expr.tuple(expr, field);
    }
  }
  
  private final Map<TokenType, ExprParser> mParsers =
    new HashMap<TokenType, ExprParser>();
}
