package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.ClassExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

public class ClassExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // Parse the class name line.
    boolean isExtend = parser.match(TokenType.EXTEND);
    if (!isExtend) parser.consume(TokenType.CLASS);
    
    String name = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    parser.consume(TokenType.LINE);
    
    ClassExpr classExpr = new ClassExpr(position, isExtend, name);
    
    // There are five kinds of things that can appear in a class body:
    // class Foo
    //     // 1. constructors
    //     this (bar Bar) ...
    //
    //     // 2. field declarations
    //     x Int
    //
    //     // 3. field definitions
    //     y = 123
    //
    //     // 4. shared field declarations
    //     shared x Int
    //
    //     // 5. shared field definitions
    //     shared y = 123
    // end
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
      if (parser.match(TokenType.THIS)) {
        // Constructor.
        FnExpr body = parser.parseFunction();
        classExpr.defineConstructor(body);
      } else {
        // Member declaration.
        boolean isShared = parser.match(TokenType.SHARED);
        String member = parser.consumeAny(TokenType.NAME).getString();
        
        // See what kind of member it is.
        if (parser.match(TokenType.EQUALS)) {
          // Field definition: "a = 123".
          Expr body = parser.parseBlock();
          classExpr.defineField(isShared, member, body);
        } else {
          // Field declaration.
          if (isShared) throw new ParseException(
              "Field declarations cannot be shared.");
          
          Expr type = parser.parseTypeExpression();
          classExpr.declareField(member, type);
        }
      }
      parser.consume(TokenType.LINE);
    }
    
    return classExpr;
  }
}
