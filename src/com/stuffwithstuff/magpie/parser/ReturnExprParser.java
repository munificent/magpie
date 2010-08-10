package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.ReturnExpr;

public class ReturnExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.RETURN);
    Position position = parser.last(1).getPosition();
    if (parser.lookAhead(TokenType.LINE)) {
      return new ReturnExpr(position, new NothingExpr(position));
    } else {
      return new ReturnExpr(position, parser.parseExpression());
    }
  }

}
