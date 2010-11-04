package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.FunctionType;

public class GetExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // TODO(bob): Support static methods.
    
    // Outside of a class expression (which handles "get") directly, a get can
    // have a couple of forms:
    //
    //    get Bar foo Int = ... // defines a getter "foo" on class "Bar"
    //    get (a b) foo Int = ... // defines a getter "foo" on the result of "a b"
    parser.consume(TokenType.GET);
    
    Expr receiver;
    if (parser.lookAhead(TokenType.NAME)) {
      // Getter on a class.
      String className = parser.consume().getString();
      receiver = Expr.name(className);
    } else {
      // Getter on a complex expression.
      parser.consume(TokenType.LEFT_PAREN);
      receiver = parser.parseExpression();
      parser.consume(TokenType.RIGHT_PAREN);
    }
    
    String name = parser.consume(TokenType.NAME).getString();

    FunctionType type;
    if (parser.lookAheadAny(TokenType.EQUALS, TokenType.LINE)) {
      // If no type is provided, default to Dynamic.
      type = FunctionType.nothingToDynamic();
    } else {
      Expr valueType = parser.parseTypeExpression();
      type = FunctionType.returningType(valueType);
    }
    
    parser.consume(TokenType.EQUALS);
    Expr body = parser.parseBlock();
    Expr function = new FnExpr(body.getPosition(), type, body);
    return Expr.message(receiver, Identifiers.DEFINE_GETTER,
        Expr.tuple(Expr.string(name), function));
  }
}
