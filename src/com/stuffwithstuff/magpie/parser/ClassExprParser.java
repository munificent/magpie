package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class ClassExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // Parse the class name line.
    boolean isExtend = parser.match(TokenType.EXTEND);
    if (!isExtend) parser.consume(TokenType.CLASS);
    
    String name = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    parser.consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();

    // Declare the class:
    // var Foo = Class newClass("Foo")
    exprs.add(new VariableExpr(position, name,
        Expr.message(Expr.name("Class"), "newClass", Expr.string(name))));
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
      // There are seven kinds of things that can appear in a class body. Each
      // gets desugared to an imperative call.
      //
      // this (bar Bar) ...   -->  Foo defineConstructor(fn(bar Bar) ...)
      // x Int                -->  Foo declareField("x", fn() Int)
      // y = 123              -->  Foo defineField("y", fn() 123)
      // shared y = 123       -->  Foo y=(123)
      // bar(arg) ...         -->  Foo defineMethod("bar", fn(arg) ...)
      // shared bar(arg) ...  -->  Foo type defineMethod("bar", fn(arg) ...)
      
      // TODO(bob): Allow method definitions in here too.
      if (parser.match(TokenType.THIS)) {
        // Constructor.
        FnExpr body = parser.parseFunction();
        exprs.add(Expr.message(Expr.name(name), Identifiers.DEFINE_CONSTRUCTOR, body));
      } else {
        // Member declaration.
        boolean isShared = parser.match(TokenType.SHARED);
        String member = parser.consumeAny(TokenType.NAME).getString();
        
        // See what kind of member it is.
        if (parser.match(TokenType.EQUALS)) {
          // Field definition: "a = 123".
          Expr body = parser.parseBlock();
          if (isShared) {
            // Just assign the field immediately.
            // Foo a=(123)
            exprs.add(Expr.message(Expr.name(name), Identifiers.makeSetter(member), body));
          } else {
            exprs.add(Expr.message(Expr.name(name), Identifiers.DEFINE_FIELD,
                Expr.tuple(Expr.string(member), Expr.fn(body))));
          }
        } else if (parser.lookAhead(TokenType.LEFT_PAREN)) {
          // Method definition.
          Expr receiver = Expr.name(name);
          if (isShared) {
            receiver = Expr.message(receiver, Identifiers.TYPE);
          }
          Expr function = parser.parseFunction();
          exprs.add(Expr.message(receiver, Identifiers.DEFINE_METHOD,
              Expr.tuple(Expr.string(member), function)));
        } else {
          // Field declaration.
          if (isShared) throw new ParseException(
              "Field declarations cannot be shared.");
          
          Expr type = parser.parseTypeExpression();
          exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_FIELD,
              Expr.tuple(Expr.string(member), Expr.fn(type))));
        }
      }
      parser.consume(TokenType.LINE);
    }
    
    return new BlockExpr(position, exprs, false);
  }
}
