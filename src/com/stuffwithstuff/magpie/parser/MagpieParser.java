package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;

public class MagpieParser extends Parser {
  public MagpieParser(Lexer lexer) {
    super(lexer);
    
    // Register the parsers for the different keywords.
    mParsers.put(TokenType.RETURN, new ReturnExprParser());
    mParsers.put(TokenType.IF, new ConditionalExprParser());
    mParsers.put(TokenType.LET, new ConditionalExprParser());
    mParsers.put(TokenType.FOR, new LoopExprParser());
    mParsers.put(TokenType.WHILE, new LoopExprParser());
    mParsers.put(TokenType.VAR, new VariableExprParser());
    mParsers.put(TokenType.CLASS, new ClassExprParser());
    mParsers.put(TokenType.EXTEND, new ClassExprParser());
    mParsers.put(TokenType.DEF, new DefineExprParser());
    mParsers.put(TokenType.SHARED, new DefineExprParser());
    mParsers.put(TokenType.TYPEOF, new TypeofExprParser());
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
    // See if we're at a keyword we know how to parse.
    ExprParser parser = mParsers.get(current().getType());
    if (parser != null) {
      return parser.parse(this);
    }
    
    // Otherwise parse a built-in expression type.
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
      return new BlockExpr(position, exprs);
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
      return new BlockExpr(position, exprs);
    } else {
      Expr expr = parseExpression();
      // Each if expression may be on its own line.
      match(TokenType.LINE);
      return expr;
    }
  }

  public Expr parseThenBlock() {
    if (match(TokenType.LINE)){
      Position position = last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!lookAhead(TokenType.ELSE) && !match(TokenType.END));
      
      position = position.union(last(1).getPosition());
      return new BlockExpr(position, exprs);
    } else {
      return parseExpression();
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
      return new BlockExpr(position, exprs);
    } else {
      return parseExpression();
    }
  }

  // fn (a) print "hi"
  public FnExpr parseFunction() {
    List<String> paramNames = new ArrayList<String>();
    FunctionType type = functionType(paramNames);
    
    Expr body = parseBlock();
    
    return new FnExpr(body.getPosition(), paramNames, type.getParamType(),
        type.getReturnType(), body);
  }

  /**
   * Parses a function type declaration. Valid examples include:
   * (->)           // takes nothing, returns nothing
   * ()             // takes nothing, returns dynamic
   * (a)            // takes a single dynamic, returns dynamic
   * (a ->)         // takes a single dynamic, returns nothing
   * (a Int -> Int) // takes and returns an int
   * 
   * @param paramNames After calling, will contain the list of parameter names.
   *                   If this is null, no parameter names will be parsed.
   *                   (This is used for inner function type declarations like
   *                   fn (Int, String ->).)
   * @return The parsed function type.
   */
  public FunctionType functionType(List<String> paramNames) {
    // Parse the prototype: (foo Foo, bar Bar -> Bang)
    consume(TokenType.LEFT_PAREN);
    
    // Parse the parameters, if any.
    List<Expr> paramTypes = new ArrayList<Expr>();
    while (!lookAheadAny(TokenType.ARROW, TokenType.RIGHT_PAREN)){
      if (paramNames != null) {
        paramNames.add(consume(TokenType.NAME).getString());
      }
      
      // TODO(bob): Need to handle named parameter with no type as a dynamic
      // parameter.
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
    
    return new FunctionType(paramType, returnType);
  }
  
  public Expr parseTypeExpression() {
    // Any Magpie expression can be used as a type declaration.
    return message();
  }
  
  private Expr assignment() {
    Expr expr = tuple();
    
    if (match(TokenType.EQUALS)) {
      // Parse the value being assigned.
      Expr value = parseExpression();

      Position position = expr.getPosition().union(value.getPosition());
      
      // TODO(bob): Need to handle tuples here too.
      if (expr instanceof MessageExpr) {
        MessageExpr message = (MessageExpr) expr;
        
        return new AssignExpr(position,
            message.getReceiver(), message.getName(), message.getArg(), value);
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
    Expr left = message();
    
    while (match(TokenType.OPERATOR)) {
      String op = last(1).getString();
      Expr right = message();

      left = new MessageExpr(left.getPosition().union(right.getPosition()),
          left, op, right);
    }
    
    return left;
  }
  
  /**
   * Parses a series of messages sends (which may or may not start with a
   * receiver like "obj message(1) andThen finally(3, 4)"
   */
  private Expr message() {
    Expr message = primary();
    
    while (true) {
      String name;
      Position position;
      Expr arg = null;
      
      if (match(TokenType.NAME)) {
        // A normal named message.
        name = last(1).getString();
        position = last(1).getPosition();
        
        // See if it has an argument.
        if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
          arg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else if (match(TokenType.LEFT_PAREN)) {
          arg = parseExpression();
          consume(TokenType.RIGHT_PAREN);
        }
      } else if (match(TokenType.LEFT_BRACKET)) {
        // An indexer expression: foo[123, 4]
        name = "[]";
        position = last(1).getPosition();
        
        // Parse the argument.
        if (match(TokenType.RIGHT_BRACKET)) {
          arg = new NothingExpr(position.union(current().getPosition()));
        } else {
          arg = parseExpression();
          consume(TokenType.RIGHT_BRACKET);
        }
      } else {
        break;
      }
      
      if (message != null) {
        position = position.union(message.getPosition());
      }
      
      if (arg != null) {
        position = position.union(arg.getPosition());
      }
      
      message = new MessageExpr(position, message, name, arg);
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
    } else if (match(TokenType.FN)) {
      return parseFunction();
    } else if (match(TokenType.NOTHING)) {
      return new NothingExpr(last(1).getPosition());
    } else if (match(TokenType.LEFT_PAREN)) {
      Expr expr = parseExpression();
      consume(TokenType.RIGHT_PAREN);
      return expr;
    } else if (match(TokenType.LEFT_BRACKET, TokenType.RIGHT_BRACKET)) {
      Position position = last(2).getPosition().union(last(1).getPosition());
      List<Expr> elements = new ArrayList<Expr>();
      return new ArrayExpr(position, elements);
    } else if (match(TokenType.LEFT_BRACKET)) {
      Position position = last(1).getPosition();
      List<Expr> elements = parseCommaList();
      consume(TokenType.RIGHT_BRACKET);
      position = position.union(last(1).getPosition());
      return new ArrayExpr(position, elements);
    }
    
    return null;
  }

  private List<Expr> parseCommaList() {
    List<Expr> exprs = new ArrayList<Expr>();
    
    do {
      exprs.add(conjunction());
    } while (match(TokenType.COMMA));
    
    return exprs;
  }

  private final Map<TokenType, ExprParser> mParsers =
    new HashMap<TokenType, ExprParser>();
}
