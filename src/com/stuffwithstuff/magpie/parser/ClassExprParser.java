package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.ApplyExpr;
import com.stuffwithstuff.magpie.ast.AssignExpr;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class ClassExprParser implements ExprParser {
  public static Expr parseClass(MagpieParser parser, boolean isExtend) {
    List<Expr> exprs = new ArrayList<Expr>();

    String className = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    Expr theClass = Expr.name(className);
    
    // Declare the class:
    if (!isExtend) {
      // var Foo = Class newClass("Foo")
      exprs.add(new VariableExpr(position, className,
          Expr.message(Expr.name("Class"), "new", Expr.string(className))));
    }
    
    // Parse the inherits clause, if any.
    if (parser.match(TokenType.COLON)) {
      String baseClass = parser.consume(TokenType.NAME).getString();
      exprs.add(new AssignExpr(position, theClass, "parent",
          Expr.name(baseClass)));
    }
    
    parser.consume(TokenType.LINE);
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
      // There are a bunch of different members that can be added to a class.
      // Each gets desugared to an imperative call:
      //
      // this(x Int) ...                  -->  Foo defineConstructor(...)
      // var foo Int = ...                -->  Foo defineField(...)
      // def foo(arg Int -> Bool) ...     -->  Foo defineMethod(...)
      // get foo Int = ...                -->  Foo defineGetter(...)
      // set foo Int = ...                -->  Foo defineSetter(...)
      // var foo Int                      -->  Foo declareField(...)
      // def foo(arg Int -> Bool)         -->  Foo declareMethod(...)
      // get foo Int                      -->  Foo declareGetter(...)
      // set foo Int                      -->  Foo declareSetter(...)
      // shared var foo Int = ...         -->  Foo type defineField(...)
      // shared foo(arg Int -> Bool) ...  -->  Foo type defineMethod(...)
      // shared get foo Int = ...         -->  Foo type defineGetter(...)
      // shared set foo Int = ...         -->  Foo type defineSetter(...)
      //
      // If none of these match, then it's presumed that we're parsing a regular
      // message send on the class object itself.
      
      if (parser.match(TokenType.THIS)) {
        exprs.add(parseConstructor(parser, theClass));
      } else if (parser.match(TokenType.VAR)) {
        exprs.add(parseField(parser, theClass));
      } else if (parser.match(TokenType.DEF)) {
        exprs.add(parseMethod(parser, theClass));
      } else if (parser.match(TokenType.GET)) {
        exprs.add(parseGetter(parser, theClass));
      } else if (parser.match(TokenType.SET)) {
        exprs.add(parseSetter(parser, theClass));
      } else if (parser.match(TokenType.SHARED)) {
        Expr metaclass = Expr.message(theClass, Identifiers.TYPE);
        // TODO(bob): Need to prevent declaring shared members: can only define.
        if (parser.match(TokenType.VAR)) {
          // TODO(bob): Need to handle defining class fields specially.
          exprs.add(parseField(parser, metaclass));
        } else if (parser.match(TokenType.DEF)) {
          exprs.add(parseMethod(parser, metaclass));
        } else if (parser.match(TokenType.GET)) {
          exprs.add(parseGetter(parser, metaclass));
        } else if (parser.match(TokenType.SET)) {
          exprs.add(parseSetter(parser, metaclass));
        }
      }
      
      // TODO(bob): arbitrary messages on class...
      
      parser.consume(TokenType.LINE);
    }
    
    return new BlockExpr(position, exprs);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.CLASS);
    return parseClass(parser, false);
  }
  
  private static Expr parseConstructor(MagpieParser parser, Expr theClass) {
    FunctionType type = parser.parseFunctionType();
    parser.consume(TokenType.EQUALS);
    Expr body = parser.parseBlock();
    Expr function = new FnExpr(body.getPosition(), type, body);

    return Expr.message(theClass, Identifiers.DEFINE_CONSTRUCTOR, function);
  }
  
  private static Expr parseField(MagpieParser parser, Expr theClass) {
    String name = parser.consume(TokenType.NAME).getString();
    Expr type = parser.parseTypeExpression();
    
    if (parser.match(TokenType.EQUALS)) {
      // Defining it.
      Expr initializer = parser.parseBlock();
      return Expr.message(theClass, Identifiers.DEFINE_FIELD,
          Expr.tuple(Expr.string(name), Expr.fn(type),
              Expr.fn(initializer)));
    } else {
      // Just declaring it.
      return Expr.message(theClass, Identifiers.DECLARE_FIELD,
          Expr.tuple(Expr.string(name), Expr.fn(type)));
    }
  }
  
  private static Expr parseMethod(MagpieParser parser, Expr theClass) {
    String name = parser.consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
    FunctionType type = parser.parseFunctionType();
    
    if (parser.match(TokenType.EQUALS)) {
      // Defining it.
      Expr body = parser.parseBlock();
      Expr function = new FnExpr(body.getPosition(), type, body);
      return Expr.message(theClass, Identifiers.DEFINE_METHOD,
          Expr.tuple(Expr.string(name), function));
    } else {
      // Just declaring it.
      Expr typeExpr = new ApplyExpr(Expr.name("Function"),
          Expr.tuple(type.getParamType(), type.getReturnType()));
      return Expr.message(theClass, Identifiers.DECLARE_METHOD,
          Expr.tuple(Expr.string(name), Expr.fn(typeExpr)));
    }
  }
  
  private static Expr parseGetter(MagpieParser parser, Expr theClass) {
    String name = parser.consume(TokenType.NAME).getString();
    
    FunctionType type;
    if (parser.lookAheadAny(TokenType.EQUALS, TokenType.LINE)) {
      // If no type is provided, default to Dynamic.
      type = FunctionType.nothingToDynamic();
    } else {
      Expr valueType = parser.parseTypeExpression();
      type = FunctionType.returningType(valueType);
    }
    
    if (parser.match(TokenType.EQUALS)) {
      // Defining it.
      Expr body = parser.parseBlock();
      Expr function = new FnExpr(body.getPosition(), type, body);
      return Expr.message(theClass, Identifiers.DEFINE_GETTER,
          Expr.tuple(Expr.string(name), function));
    } else {
      // Just declaring it.
      Expr typeExpr = new ApplyExpr(Expr.name("Function"),
          Expr.tuple(type.getParamType(), type.getReturnType()));
      return Expr.message(theClass, Identifiers.DECLARE_GETTER,
          Expr.tuple(Expr.string(name), Expr.fn(typeExpr)));
    }
  }
  
  private static Expr parseSetter(MagpieParser parser, Expr theClass) {
    // TODO(bob): Implement me!
    return new NothingExpr(Position.none());
  }
}
