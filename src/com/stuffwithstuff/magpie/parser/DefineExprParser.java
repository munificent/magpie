package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class DefineExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // TODO(bob): Support static functions.
    
    // Outside of a class expression (which handles "def") directly, a def can
    // have a couple of forms:
    //
    //    def foo(a) ...     // defines a local function "foo"
    //    def Bar foo(a) ... // defines a method "foo" on class "Bar"
    //    def (a b) foo(a) ... // defines a method "foo" on the result of "a b"
    Position position = parser.consume(TokenType.DEF).getPosition();
    
    if (parser.lookAhead(TokenType.NAME, TokenType.LEFT_PAREN)) {
      // Local function.
      String name = parser.consume().getString();
      FnExpr function = parser.parseFunction();
      return new VariableExpr(position.union(function.getPosition()),
          name, function);
    }
    
    Expr receiver;
    if (parser.lookAhead(TokenType.NAME)) {
      // Method on a class.
      String className = parser.consume().getString();
      receiver = Expr.name(className);
    } else {
      // Method on a complex expression.
      parser.consume(TokenType.LEFT_PAREN);
      receiver = parser.parseExpression();
      parser.consume(TokenType.RIGHT_PAREN);
    }
    
    String name = parser.consumeAny(
        TokenType.NAME, TokenType.OPERATOR).getString();
    FnExpr function = parser.parseFunction();
      
    return Expr.message(receiver, Identifiers.DEFINE_METHOD,
        Expr.tuple(Expr.string(name), function));
  }
}
