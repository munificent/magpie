package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.util.Pair;

public class IfExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.consume(TokenType.IF).getPosition();
    
    // Parse the condition.
    Expr condition = parser.parseBlock(TokenType.THEN).getKey();
    
    // Parse the then body.
    parser.consume(TokenType.THEN);
    Pair<Expr, TokenType> thenParse = parser.parseBlock(TokenType.END, TokenType.ELSE);
    Expr thenExpr = thenParse.getKey();

    // Parse the else body.
    Expr elseExpr;
    if ((thenParse.getValue() != TokenType.END) && parser.match(TokenType.ELSE)) {
      elseExpr = parser.parseEndBlock();
    } else {
      elseExpr = new NothingExpr(parser.last(1).getPosition());
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        null, condition, thenExpr, elseExpr);
  }
}
