package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParser extends Parser {  
  public MagpieParser(Lexer lexer, Map<String, ExprParser> parsewords,
      Set<String> keywords) {
    super(lexer);
    
    // Register the parsers for the different keywords.
    // TODO(bob): Eventually these should all go away.
    mParsers.put(TokenType.FOR, new LoopExprParser());
    mParsers.put(TokenType.MATCH, new MatchExprParser());
    mParsers.put(TokenType.WHILE, new LoopExprParser());
    
    mKeywordParsers = parsewords;
    mKeywords = keywords;
    
    if (mKeywordParsers != null) {
      mKeywordParsers.put("class", new ClassExprParser());
      mKeywordParsers.put("extend", new ExtendExprParser());
      mKeywordParsers.put("interface", new InterfaceExprParser());
    }
    
    if (mKeywords != null) {
      mKeywords.add("do");
    }
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
  
  // TODO(bob): Get rid of this when static functions go and use the pattern one
  // for everything.
  // fn (a) print "hi"
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
    
    // Wrap the body in a dynamic function.
    if (type != null) {
      expr = Expr.fn(position, type, expr);
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
          constraint = TypeParser.parse(this);
        }
        typeParams.add(new Pair<String, Expr>(name, constraint));
      } while (match(TokenType.COMMA));
      
      consume(TokenType.RIGHT_BRACKET);
    }
    
    // Parse the prototype: (foo Foo, bar Bar -> Bang)
    consume(TokenType.LEFT_PAREN);
    
    // Parse the parameter pattern, if any.
    Pattern pattern = null;
    if (!lookAheadAny(TokenType.ARROW, TokenType.RIGHT_PAREN)) {
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
      consume(TokenType.ARROW);
      
      if (lookAhead(TokenType.RIGHT_PAREN)) {
        // An arrow, but no return type, so infer nothing.
        returnType = Expr.name("Nothing");
      } else {
        returnType = TypeParser.parse(this);
      }
      consume(TokenType.RIGHT_PAREN);
    }
    
    return new FunctionType(typeParams, pattern, returnType);
  }
  
  public String parseFunctionName() {
    return consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
  }
  
  public Expr parseOperator() {
    return operator();
  }
  
  @Override
  protected boolean isKeyword(String name) {
    return ((mKeywordParsers != null) && mKeywordParsers.containsKey(name)) ||
           ((mKeywords != null) && mKeywords.contains(name));
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
        Position position = current().getPosition();
        List<MatchCase> catches = new ArrayList<MatchCase>();
        while (match(TokenType.CATCH)) {
          catches.add(parseCatch(keyword1, keyword2, endTokens));
        }
        
        // TODO(bob): This is kind of hokey.
        if (catches.size() > 0) {
          Expr valueExpr = Expr.name("__err__");
          Expr elseExpr = Expr.message(Expr.name("Runtime"), "throw", valueExpr);
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

  private MatchCase parseCatch(String keyword1, String keyword2, TokenType... endTokens) {
    Pattern pattern = PatternParser.parse(this);

    consume(TokenType.THEN);
    
    Pair<Expr, Token> body = parseBlock(false, keyword1, keyword2, endTokens);
    
    // Allow newlines to separate single-line catches.
    if ((body.getValue() == null) &&
        lookAhead(TokenType.LINE, TokenType.CATCH)) {
      consume();
    }
    
    return new MatchCase(pattern, body.getKey());
  }
  
  private Expr assignment() {
    Expr expr = composite();
    
    if (match(TokenType.EQUALS)) {
      // Parse the value being assigned.
      Expr value = parseExpression();

      return ConvertAssignmentExpr.convert(expr, value);
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
      
      return Expr.record(position, fields);
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
        // A call with type arguments.
        List<Expr> typeArgs = new ArrayList<Expr>();
        do {
          typeArgs.add(TypeParser.parse(this));
        } while(match(TokenType.COMMA));
        consume(TokenType.RIGHT_BRACKET);
        
        // See if there is a regular argument too.
        Expr arg;
        if (match(TokenType.LEFT_PAREN)) {
          arg = parseExpression();
          consume(TokenType.RIGHT_PAREN);
        } else {
          arg = Expr.nothing();
        }
        message = Expr.call(message, typeArgs, arg);
      } else if (lookAhead(TokenType.LEFT_PAREN)) {
        // A function call like foo(123).
        Expr arg = groupExpression(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN);
        message = Expr.call(message, arg);
      } else if (match(TokenType.WITH)) {
        // Parse the parameter list if given.
        FunctionType blockType;
        if (lookAhead(TokenType.LEFT_PAREN)) {
          blockType = parseFunctionType();
        } else {
          // Else just assume a single "it" parameter.
          blockType = (new FunctionType(new VariablePattern(Name.IT, null),
              Expr.name("Dynamic")));
        }

        // Parse the block and wrap it in a function.
        Expr block = parseEndBlock();
        block = Expr.fn(block.getPosition(), blockType, block);
        
        // Apply it to the previous expression.
        if (message instanceof CallExpr) {
          // foo(123) with ...  --> Call(Msg(foo), Tuple(123, block))
          CallExpr call = (CallExpr)message;
          Expr arg = addTupleField(call.getArg(), block);
          message = Expr.call(call.getTarget(), arg);
        } else {
          // 123 with ...  --> Call(Int(123), block)
          message = Expr.call(message, block);
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
    }

    if (match(TokenType.INT)) {
      return Expr.int_(last(1).getPosition(), last(1).getInt());
    }

    if (match(TokenType.STRING)) {
      return Expr.string(last(1).getPosition(), last(1).getString());
    }

    if (match(TokenType.THIS)) {
      return Expr.this_(last(1).getPosition());
    }

    if (match(TokenType.NOTHING)) {
      return Expr.nothing(last(1).getPosition());
    }
    
    if (match(TokenType.FN)) {
      return parsePatternFunction();
    }
    
    if (match("do")) {
      Expr body = parseEndBlock();
      return Expr.scope(body);
    }

    if ((mQuoteDepth > 0) && match(TokenType.BACKTICK)) {
      Position position = last(1).getPosition();
      Expr body;
      if (match(TokenType.NAME)) {
        body = Expr.message(last(1).getPosition(), null, last(1).getString());
      } else {
        body = groupExpression(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN);
      }
      return new UnquoteExpr(position, body);
    }

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = parseExpression();
      consume(TokenType.RIGHT_PAREN);
      return expr;
    }

    if (lookAhead(TokenType.LEFT_BRACE)) {
      mQuoteDepth++;
      Position position = current().getPosition();
      Expr expr = groupExpression(TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE);
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

  private Expr parsePatternFunction() {
    FunctionType type;
    
    // TODO(bob): Fallback to old stuff for static functions. Once patterns are
    // working, I can implement 'T-style generics, and static functions can go
    // away completely.
    if (lookAhead(TokenType.LEFT_BRACKET)) {
      return parseFunction();
    }
    
    // Parse the type signature.
    if (match(TokenType.LEFT_PAREN)) {
      Pattern pattern = null;
      if (!lookAheadAny(TokenType.ARROW, TokenType.RIGHT_PAREN)) {
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
        consume(TokenType.ARROW);
        
        if (lookAhead(TokenType.RIGHT_PAREN)) {
          // An arrow, but no return type, so infer nothing.
          returnType = Expr.name("Nothing");
        } else {
          returnType = TypeParser.parse(this);
        }
        consume(TokenType.RIGHT_PAREN);
      }
      
      type = new FunctionType(pattern, returnType);
    } else {
      // No type signature provided, so infer (_ -> Dynamic)
      type = FunctionType.nothingToDynamic();
    }
    
    Expr body = parseEndBlock();
    
    return Expr.fn(body.getPosition(), type, body);
  }

  private Expr groupExpression(TokenType left, TokenType right) {
    consume(left);
    
    if (match(right)) {
      return Expr.nothing(Position.surrounding(last(2), last(1)));
    }
    
    Expr expr = parseExpression();
    
    // Allow a newline before the final ).
    match(TokenType.LINE);
    consume(right);
    
    return expr;
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
