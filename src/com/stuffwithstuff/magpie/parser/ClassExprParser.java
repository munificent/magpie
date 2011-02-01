package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.NotImplementedException;

public class ClassExprParser implements ExprParser {
  public static Expr parseClass(MagpieParser parser, boolean isExtend) {
    String className = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    // A class expression is desugared to a call to "receiving" on the class
    // object. For a new class, that's like:
    //
    //  class Foo
    //      var bar Int = 123
    //  end
    //
    //  (var Foo = Class new("Foo")) receiving with
    //      this defineField("bar", ...)
    //  end
    //
    // For an extended class, it's simply:
    //
    //  extend class Foo
    //      var bar Int = 123
    //  end
    //
    //  Foo receiving with
    //      this defineField("bar", ...)
    //  end
    
    Expr classReceiver;
    if (isExtend) {
      // Foo
      classReceiver = Expr.name(position, className);
    } else {
      // var Foo = Class new("Foo")
      classReceiver = Expr.var(position, className,
          Expr.message(position, Expr.name(position, "Class"),
          Name.NEW, Expr.string(className)));
    }

    parser.consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();
    Expr theClass = Expr.this_(position);
    
    // Parse the body.
    while (!parser.match("end")) {
      // There are a bunch of different members that can be added to a class.
      // Each gets desugared to an imperative call:
      //
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
      // expression evaluated in a context where "this" is the class.
      
      if (parser.match("delegate", "var")) {
        exprs.add(parseField(parser, true, theClass));
      } else if (parser.match("var")) {
        exprs.add(parseField(parser, false, theClass));
      } else if (parser.match("def")) {
        exprs.add(parseMethod(parser, theClass));
      } else if (parser.match("get")) {
        exprs.add(parseGetter(parser, theClass));
      } else if (parser.match("set")) {
        exprs.add(parseSetter(parser, theClass));
      } else if (parser.match("shared")) {
        Expr metaclass = Expr.message(theClass, Name.TYPE);
        // TODO(bob): Need to prevent declaring shared members: can only define.
        if (parser.match("delegate", "var")) {
          // TODO(bob): Need to handle defining class fields specially.
          exprs.add(parseField(parser, true, metaclass));
        } else if (parser.match("var")) {
          // TODO(bob): Need to handle defining class fields specially.
          exprs.add(parseField(parser, false, metaclass));
        } else if (parser.match("def")) {
          exprs.add(parseMethod(parser, metaclass));
        } else if (parser.match("get")) {
          exprs.add(parseGetter(parser, metaclass));
        } else if (parser.match("set")) {
          exprs.add(parseSetter(parser, metaclass));
        }
      } else {
        exprs.add(parser.parseExpression());
      }
      
      parser.consume(TokenType.LINE);
    }
    
    // Wrap the body in a function.
    Expr body = Expr.fn(Expr.block(exprs));
    
    // Make the class receive it.
    return Expr.message(classReceiver, Name.RECEIVING, body);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume("class");
    return parseClass(parser, false);
  }
  
  private static Expr parseField(MagpieParser parser, boolean isDelegate,
      Expr theClass) {
    String name = parser.consume(TokenType.NAME).getString();
    
    // Parse the type annotation if there is one.
    Expr type;
    if (parser.lookAhead(TokenType.EQUALS)) {
      type = Expr.nothing();
    } else {
      Expr typeExpr = TypeParser.parse(parser);
      type = Expr.quote(typeExpr.getPosition(), typeExpr);
    }
    
    if (parser.match(TokenType.EQUALS)) {
      // Defining it.
      Expr initializer = parser.parseEndBlock();
      return Expr.message(theClass, Name.DEFINE_FIELD,
          Expr.tuple(Expr.string(name), Expr.bool(isDelegate), type,
              Expr.fn(initializer)));
    } else {
      // Just declaring it.
      return Expr.message(theClass, Name.DECLARE_FIELD,
          Expr.tuple(Expr.string(name), Expr.bool(isDelegate), type));
    }
  }
  
  private static Expr parseMethod(MagpieParser parser, Expr theClass) {
    String name = parser.consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
    Expr function = parser.parseFunction();
    return Expr.message(theClass, Name.DEFINE_METHOD,
        Expr.tuple(Expr.string(name), function));
  }
  
  private static Expr parseGetter(MagpieParser parser, Expr theClass) {
    String name = parser.consume(TokenType.NAME).getString();
    
    Expr type;
    if (parser.lookAheadAny(TokenType.EQUALS, TokenType.LINE)) {
      // If no type is provided, default to Dynamic.
      type = Expr.name("Dynamic");
    } else {
      type = TypeParser.parse(parser);
    }
    
    if (parser.match(TokenType.EQUALS)) {
      // Defining it.
      Expr body = parser.parseEndBlock();
      FunctionType fnType = FunctionType.returningType(type);
      Expr function = Expr.fn(body.getPosition(), fnType, body);
      return Expr.message(theClass, Name.DEFINE_GETTER,
          Expr.tuple(Expr.string(name), function));
    } else {
      // Just declaring it.
      return Expr.message(theClass, Name.DECLARE_GETTER,
          Expr.tuple(Expr.string(name), Expr.fn(type)));
    }
  }
  
  private static Expr parseSetter(MagpieParser parser, Expr theClass) {
    throw new NotImplementedException();
  }
}
