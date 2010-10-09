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
      
      // TODO(bob): Need to handle tuples here too.
      if (expr instanceof MessageExpr) {
        MessageExpr message = (MessageExpr) expr;
        
        // Based on whether or not the left-hand side has a target or an
        // argument, there are four different flavors of assignment. The
        // simplest is no target and no target argument, like:
        //
        //    name = value
        //
        // This is the only case that actually becomes an assignment expression.
        // The other cases all desugar to a regular method call like so:
        //
        // name = value              -->  name = value
        // name(arg) = value         -->  name call=(arg, value)
        // target name = value       -->  target name=(value)
        // target name(arg) = value  -->  target name=(arg, value)

        if (message.getReceiver() == null) {
          if (message.getArg() == null) {
            // name = value
            return new AssignExpr(position, message.getName(), value);
          } else {
            // name(arg) = value  -->  name call=(arg, value)
            return new MessageExpr(position, Expr.name(message.getName()),
                Identifiers.CALL_ASSIGN, Expr.tuple(message.getArg(), value));
          }
        } else {
          if (message.getArg() == null) {
            // target name = value  -->  target name=(value)
            return new MessageExpr(position, message.getReceiver(),
                Identifiers.makeSetter(message.getName()), value);
          } else {
            // target name(arg) = value  -->  target name=(arg, value)
            return new MessageExpr(position, message.getReceiver(),
                Identifiers.makeSetter(message.getName()),
                Expr.tuple(message.getArg(), value));
          }
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
      Position position = current().getPosition();
      Expr staticArg = null;
      Expr arg = null;
      
      // TODO(bob): Hackish. If we encounter a name: pair, then we're in the
      // middle of an object literal, so don't treat the name as a message.
      if (lookAhead(TokenType.NAME, TokenType.COLON)) break;
      
      // TODO(bob): This is kind of gross. The static arg stuff is just jammed
      // in here awkwardly. Should refactor.
      if (match(TokenType.NAME)) {
        // A normal named message.
        name = last(1).getString();
        
        // See if it has an argument.
        if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
          arg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else if (match(TokenType.LEFT_PAREN)) {
          arg = parseExpression();
          consume(TokenType.RIGHT_PAREN);
        }
      } else if (lookAhead(TokenType.LEFT_PAREN)) {
        // A call (i.e. an unnamed message like 123(345) or (foo bar)[baz](bang).
        name = "call";
        
        // Pass the argument if present.
        if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
          arg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else if(match(TokenType.LEFT_PAREN)) {
          arg = parseExpression();
          consume(TokenType.RIGHT_PAREN);
        }
        
      } else if (match(TokenType.LEFT_BRACKET)) {
        // A static function call (i.e. foo[123]).
        name = "not used";

        if (match(TokenType.RIGHT_BRACKET)) {
          staticArg = new NothingExpr(last(2).getPosition().union(last(1).getPosition()));
        } else {
          staticArg = parseExpression();
          consume(TokenType.RIGHT_BRACKET);
        }
      } else {
        break;
      }
      
      // Look for a following block argument.
      if (match(TokenType.WITH)) {
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
        Expr blockArg = parseBlock();
        blockArg = new FnExpr(blockArg.getPosition(), blockType, blockArg);
        
        // Tack it on to the regular argument.
        if (arg == null) {
          arg = blockArg;
        } else if (arg instanceof TupleExpr) {
          ((TupleExpr)arg).getFields().add(blockArg);
        } else {
          arg = Expr.tuple(arg, blockArg);
        }
      }
      
      position = position.union(last(1).getPosition());
      
      if (staticArg != null) {
        message = new InstantiateExpr(position, message, staticArg);
      } else {
        message = new MessageExpr(position, message, name, arg);
      }
    }
    
    if (message == null) {
      throw new ParseException("Could not parse expression at " + current().getPosition());
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

  private final Map<TokenType, ExprParser> mParsers =
    new HashMap<TokenType, ExprParser>();
}
