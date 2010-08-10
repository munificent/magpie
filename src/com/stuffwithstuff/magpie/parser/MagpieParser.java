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
    mParsers.put(TokenType.VAR, new DefineExprParser());
    mParsers.put(TokenType.CLASS, new ClassExprParser());
    mParsers.put(TokenType.EXTEND, new ClassExprParser());
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
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (!match(TokenType.END)) {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      }
            
      return new BlockExpr(exprs);
    } else {
      return parseExpression();
    }
  }

  public Expr parseIfBlock() {
    if (match(TokenType.LINE)){
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!lookAhead(TokenType.THEN));
      
      match(TokenType.LINE);

      return new BlockExpr(exprs);
    } else {
      Expr expr = parseExpression();
      // Each if expression may be on its own line.
      match(TokenType.LINE);
      return expr;
    }
  }

  public Expr parseThenBlock() {
    if (match(TokenType.LINE)){
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!lookAhead(TokenType.ELSE) && !match(TokenType.END));
      
      return new BlockExpr(exprs);
    } else {
      return parseExpression();
    }
  }
  
  public Expr parseElseBlock() {
    if (match(TokenType.LINE)){
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parseExpression());
        consume(TokenType.LINE);
      } while (!match(TokenType.END));
      
      return new BlockExpr(exprs);
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
      paramTypes.add(parseTypeExpression());
      
      if (!match(TokenType.COMMA)) break;
    }
    
    // Aggregate the parameter types into a single type.
    Expr paramType = null;
    switch (paramTypes.size()) {
    case 0:  paramType = new NameExpr("Nothing"); break;
    case 1:  paramType = paramTypes.get(0); break;
    default: paramType = new TupleExpr(paramTypes);
    }
    
    // Parse the return type, if any.
    Expr returnType = null;
    if (match(TokenType.RIGHT_PAREN)) {
      // No return type, so infer dynamic.
      returnType = new NameExpr("Dynamic");
    } else {
      consume(TokenType.ARROW);
      
      if (lookAhead(TokenType.RIGHT_PAREN)) {
        // An arrow, but no return type, so infer nothing.
        returnType = new NameExpr("Nothing");
      } else {
        returnType = parseTypeExpression();
      }
      consume(TokenType.RIGHT_PAREN);
    }
    
    return new FunctionType(paramType, returnType);
  }
  
  public Expr parseTypeExpression() {
    // Any Magpie expression can be used as a type declaration. If omitted, it
    // defaults to dynamically typed.
    Expr type = primary();
    if (type != null) return type;
    
    return new NameExpr("Dynamic");
  }
  
  private Expr assignment() {
    Expr expr = tuple();
    
    if (match(TokenType.EQUALS)) {
      // Parse the value being assigned.
      Expr value = parseExpression();

      Position position = Position.union(expr.getPosition(), value.getPosition());
      
      // Transform the left-hand expression into an assignment form. Examples:
      // a = v       ->  a = v      AssignExpr(null, "a", null, v)
      // a.b = v     ->  a.b=(v)    AssignExpr(a,    "b", null, v)
      // a.b.c = v   ->  a.b.c=(v)  AssignExpr(a.b,  "c", null, v)
      // a.b c = v   ->  a.b=(c, v) AssignExpr(a,    "b", c,    v)
      // a ^ b = v   ->  a.^=(b)    AssignExpr(a,    "^", b,    v)
      // Other expression forms on the left-hand side are considered invalid and
      // will throw a parse exception.
      // TODO(bob): Need to handle tuples here too.
      
      if (expr instanceof NameExpr) {
        return new AssignExpr(position, null, ((NameExpr)expr).getName(), null, value);
      } else if (expr instanceof MethodExpr) {
        MethodExpr method = (MethodExpr) expr;
        
        if (method.getArg() instanceof NothingExpr) {
          // a.b = v -> a.b=(v)
          return new AssignExpr(position, method.getReceiver(), method.getMethod(), null, value);
        } else {
          // a.b c = v -> a.b=(c, v)
          return new AssignExpr(position, method.getReceiver(), method.getMethod(),
              method.getArg(), value);
        }
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
    List<Expr> fields = new ArrayList<Expr>();
    
    do {
      fields.add(operator());
    } while (match(TokenType.COMMA));
    
    // Only wrap in a tuple if there are multiple fields.
    if (fields.size() == 1) return fields.get(0);
    
    return new TupleExpr(fields);
  }

  /**
   * Parses a series of operator expressions like "a + b - c".
   */
  private Expr operator() {
    Expr left = call();
    if (left == null) {
      throw new ParseException(":(");
    }
    
    while (match(TokenType.OPERATOR)) {
      String op = last(1).getString();
      Expr right = call();
      if (right == null) {
        throw new ParseException(":(");
      }

      left = new MethodExpr(left, op, right);
    }
    
    return left;
  }

  // The next two functions are a bit squirrely. Function calls like "abs 123"
  // are generally lower precedence than method calls like "123.abs". However,
  // they interact with each other. Some examples will clarify:
  // a b c d  ->  a(b(c(d)))
  // a b c.d  ->  a(b(c.d())
  // a b.c d  ->  a(b.c(d))
  // a b.c.d  ->  a(b.c().d())
  // a.b c d  ->  a.b(c(d))
  // a.b c.d  ->  a.b(c.d())
  // a.b.c d  ->  a.b().c(d)
  // a.b.c.d  ->  a.b().c().d()
  
  /**
   * Parses a series of function calls like "foo bar bang".
   * @return The parsed expression or null if unsuccessful.
   */
  private Expr call() {
    Expr expr = method();
    if (expr == null) return null;
    
    Expr arg = call();
    if (arg == null) return expr;
    
    return new CallExpr(expr, arg);
  }
  
  /**
   * Parses a series of method calls like "foo.bar.bang".
   * @return The parsed expression or null if unsuccessful.
   */
  private Expr method() {
    Expr receiver = primary();
    if (receiver == null) return null;
    
    while (match(TokenType.DOT)) {
      if (match(TokenType.NAME)) {
        // Regular named method: foo.bar
        String method = last(1).getString();

        Expr arg = call();
        if (arg == null) {
          // If the argument is omitted, infer ()
          arg = new NothingExpr(last(1).getPosition());
        }
        receiver = new MethodExpr(receiver, method, arg);
      } else {
        // Functor object: foo.123
        Expr arg = primary();
        receiver = new MethodExpr(receiver, "apply", arg);
      }
    }
    
    return receiver;
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
    } else if (match(TokenType.NAME)) {
      return new NameExpr(last(1));
    } else if (match(TokenType.THIS)) {
      return new ThisExpr(last(1).getPosition());
    } else if (match(TokenType.FN)) {
      return parseFunction();
    } else if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
      return new NothingExpr(
          Position.union(last(2).getPosition(), last(1).getPosition()));
    } else if (match(TokenType.LEFT_PAREN)) {
      Expr expr = parseExpression();
      consume(TokenType.RIGHT_PAREN);
      return expr;
    }
    
    return null;
  }
  
  private final Map<TokenType, ExprParser> mParsers =
    new HashMap<TokenType, ExprParser>();
}
