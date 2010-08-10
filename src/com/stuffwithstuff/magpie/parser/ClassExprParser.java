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
    
    // There are four kinds of things that can appear in a class body:
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
    //     // 4. method definitions
    //     doSomething (a Int) ...
    //     + (other) ...
    //
    //     // 5. shared field declarations
    //     shared x Int
    //
    //     // 6. shared field definitions
    //     shared y = 123
    //
    //     // 7. shared method definitions
    //     shared doSomething (a Int) ...
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
        String member = parser.consumeAny(TokenType.NAME, TokenType.OPERATOR)
            .getString();
        
        // See what kind of member it is.
        if (parser.match(TokenType.EQUALS)) {
          // Field definition: "a = 123".
          Expr body = parser.parseBlock();
          classExpr.defineField(isShared, member, body);
        }
        else if (parser.lookAhead(TokenType.LEFT_PAREN)) {
          // Method definition: "foo () print 123".
          FnExpr function = parser.parseFunction();
          classExpr.defineMethod(isShared, member, function);
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
