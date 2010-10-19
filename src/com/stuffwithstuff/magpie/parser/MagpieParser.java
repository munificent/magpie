package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {
  public MagpieParser(Lexer lexer) {
    super(lexer);
    
    // Register the parsers for the different keywords.
    mParsers.put(TokenType.BREAK, new BreakExprParser());
    mParsers.put(TokenType.CLASS, new ClassExprParser());
    mParsers.put(TokenType.DEF, new DefineExprParser());
    mParsers.put(TokenType.EXTEND, new ExtendExprParser());
    mParsers.put(TokenType.FN, new FnExprParser());
    mParsers.put(TokenType.FOR, new LoopExprParser());
    mParsers.put(TokenType.IF, new ConditionalExprParser());
    mParsers.put(TokenType.INTERFACE, new InterfaceExprParser());
    mParsers.put(TokenType.LET, new ConditionalExprParser());
    mParsers.put(TokenType.RETURN, new ReturnExprParser());
    mParsers.put(TokenType.SHARED, new DefineExprParser());
    mParsers.put(TokenType.TYPEOF, new TypeofExprParser());
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

  // TODO(bob): There's a lot of overlap in the next four functions, but,
  //            unfortunately also some slight differences. It would be cool to
  //            unify these somehow.
  
  public Expr parseBlock() {
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (!match(TokenType.END)) {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      }
      
      position = position.union(last(1).getPosition());
      return new BlockExpr(position, exprs, true);
    } else {
      return parseExpression();
    }
  }

  public Expr parseIfBlock() {
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!lookAhead(TokenType.THEN));
      
      match(TokenType.LINE);

      position = position.union(last(1).getPosition());
      return new BlockExpr(position, exprs, true);
    } else {
      Expr expr = parseExpression();
      // Each if expression may be on its own line.
      match(TokenType.LINE);
      return expr;
    }
  }
  
  public Expr parseElseBlock() {
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!match(TokenType.END));
      
      position = position.union(last(1).getPosition());
      return new BlockExpr(position, exprs, true);
    } else {
      return parseExpression();
    }
  }

  // fn (a) print "hi"
  public FnExpr parseFunction() {
    // If () is omitted, infer it.
    FunctionType type;
    if (lookAhead(TokenType.LEFT_PAREN)) {
      type = parseFunctionType();
    } else {
      type = FunctionType.nothingToDynamic();
    }
    Expr body = parseBlock();
    
    return new FnExpr(body.getPosition(), type, body);
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
    // Parse the prototype: (foo Foo, bar Bar -> Bang)
    consume(TokenType.LEFT_PAREN);
    
    // Parse the parameters, if any.
    List<String> paramNames = new ArrayList<String>();
    List<Expr> paramTypes = new ArrayList<Expr>();
    while (!lookAheadAny(TokenType.ARROW, TokenType.RIGHT_PAREN)){
      paramNames.add(consume(TokenType.NAME).getString());
      
      if (!lookAheadAny(TokenType.ARROW, TokenType.COMMA, TokenType.RIGHT_PAREN)) {
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
    if (match(TokenType.RIGHT_PAREN)) {
      // No return type, so infer dynamic.
      returnType = Expr.name("Dynamic");
    } else {
      consume(TokenType.ARROW);
      
      if (lookAhead(TokenType.RIGHT_PAREN)) {
        // An arrow, but no return type, so infer nothing.
        returnType = Expr.name("Nothing");
      } else {
        returnType = parseTypeExpression();
      }
      consume(TokenType.RIGHT_PAREN);
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
            Expr.tuple(apply.getArg(), value));
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
      block = new FnExpr(block.getPosition(), blockType, block);
      
      // Apply it to the previous expression.
      if (expr instanceof ApplyExpr) {
        // foo(123) with ...  --> Apply(Msg(foo), Tuple(123, block))
        ApplyExpr apply = (ApplyExpr)expr;
        Expr arg = addTupleField(apply.getArg(), block);
        expr = new ApplyExpr(apply.getTarget(), arg);
      } else {
        // 123 with ...  --> Apply(Int(123), block)
        expr = new ApplyExpr(expr, block);
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
      Position start = current().getPosition();
      
      if (match(TokenType.NAME)) {
        message = new MessageExpr(last(1).getPosition(), message,
            last(1).getString());
      } else if (match(TokenType.LEFT_BRACKET)) {
        // A static apply (i.e. foo[123]).
        Expr staticArg;
        if (match(TokenType.RIGHT_BRACKET)) {
          staticArg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else {
          staticArg = parseExpression();
          consume(TokenType.RIGHT_BRACKET);
        }
        message = new InstantiateExpr(start.union(current().getPosition()),
            message, staticArg);
      } else {
        Expr arg = primary();
        if (arg == null) break;
        message = new ApplyExpr(message, arg);
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
