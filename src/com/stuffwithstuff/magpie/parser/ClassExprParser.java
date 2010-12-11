package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.ThisExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

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
      classReceiver = new VariableExpr(position, className,
          Expr.message(position, Expr.name(position, "Class"),
          Identifiers.NEW, Expr.string(className)));
    }

    parser.consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();
    Expr theClass = new ThisExpr(position);
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
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
      
      if (parser.match(TokenType.DELEGATE, TokenType.VAR)) {
        exprs.add(parseField(parser, true, theClass));
      } else if (parser.match(TokenType.VAR)) {
        exprs.add(parseField(parser, false, theClass));
      } else if (parser.match(TokenType.DEF)) {
        exprs.add(parseMethod(parser, theClass));
      } else if (parser.match(TokenType.GET)) {
        exprs.add(parseGetter(parser, theClass));
      } else if (parser.match(TokenType.SET)) {
        exprs.add(parseSetter(parser, theClass));
      } else if (parser.match(TokenType.SHARED)) {
        Expr metaclass = Expr.message(theClass, Identifiers.TYPE);
        // TODO(bob): Need to prevent declaring shared members: can only define.
        if (parser.match(TokenType.DELEGATE, TokenType.VAR)) {
          // TODO(bob): Need to handle defining class fields specially.
          exprs.add(parseField(parser, true, metaclass));
        } else if (parser.match(TokenType.VAR)) {
          // TODO(bob): Need to handle defining class fields specially.
          exprs.add(parseField(parser, false, metaclass));
        } else if (parser.match(TokenType.DEF)) {
          exprs.add(parseMethod(parser, metaclass));
        } else if (parser.match(TokenType.GET)) {
          exprs.add(parseGetter(parser, metaclass));
        } else if (parser.match(TokenType.SET)) {
          exprs.add(parseSetter(parser, metaclass));
        }
      } else {
        exprs.add(parser.parseExpression());
      }
      
      parser.consume(TokenType.LINE);
    }
    
    // Wrap the body in a function.
    Expr body = Expr.fn(new BlockExpr(position, exprs));
    
    // Make the class receive it.
    return Expr.message(classReceiver, Identifiers.RECEIVING, body);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.CLASS);
    return parseClass(parser, false);
  }
  
  private static Expr parseField(MagpieParser parser, boolean isDelegate,
      Expr theClass) {
    String name = parser.consume(TokenType.NAME).getString();
    Expr type = parser.parseTypeExpression();
    
    if (parser.match(TokenType.EQUALS)) {
      // TODO(bob): Having to declare a type when we have an initializer is
      // lame. It should be able to infer it. The reason it can't right now is
      // because the mirroring getters and setters declared with the field need
      // type annotations, but that should be solvable by just storing the
      // initializer with them too so they can evaluate its type to get their
      // type.
      
      // Defining it.
      Expr initializer = parser.parseBlock();
      return Expr.message(theClass, Identifiers.DEFINE_FIELD,
          Expr.tuple(Expr.string(name), Expr.bool(isDelegate), Expr.fn(type),
              Expr.fn(initializer)));
    } else {
      // Just declaring it.
      return Expr.message(theClass, Identifiers.DECLARE_FIELD,
          Expr.tuple(Expr.string(name), Expr.bool(isDelegate), Expr.fn(type)));
    }
  }
  
  private static Expr parseMethod(MagpieParser parser, Expr theClass) {
    String name = parser.consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
    Expr function = parser.parseFunction();
    return Expr.message(theClass, Identifiers.DEFINE_METHOD,
        Expr.tuple(Expr.string(name), function));
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
      Expr function = new FnExpr(body.getPosition(), type, body, false);
      return Expr.message(theClass, Identifiers.DEFINE_GETTER,
          Expr.tuple(Expr.string(name), function));
    } else {
      // Just declaring it.
      Expr typeExpr = Expr.message(Expr.name("Function"), Identifiers.NEW_TYPE,
          Expr.tuple(type.getParamType(), type.getReturnType(), Expr.bool(false)));
      return Expr.message(theClass, Identifiers.DECLARE_GETTER,
          Expr.tuple(Expr.string(name), Expr.fn(typeExpr)));
    }
  }
  
  private static Expr parseSetter(MagpieParser parser, Expr theClass) {
    // TODO(bob): Implement me!
    return new NothingExpr(Position.none());
  }
}
