package com.stuffwithstuff.magpie.parser;

import java.util.EnumSet;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.parser.MagpieParser.BlockOptions;
import com.stuffwithstuff.magpie.util.Ref;

public class IfExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.consume(TokenType.IF).getPosition();
    
    // Parse the condition.
    Expr condition = parser.parseBlock(EnumSet.of(BlockOptions.CONSUME_END),
        TokenType.THEN);
    
    // Parse the then body.
    parser.consume(TokenType.THEN);
    Ref<Boolean> consumedEnd = new Ref<Boolean>();
    Expr thenExpr = parser.parseBlock(consumedEnd,
        EnumSet.of(BlockOptions.CONSUME_END), TokenType.ELSE);
    
    // Parse the else body.
    Expr elseExpr = null;
    if (!consumedEnd.get() && parser.match(TokenType.ELSE)) {
      elseExpr = parser.parseBlock();
    } else {
      elseExpr = new NothingExpr(parser.last(1).getPosition());
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        null, condition, thenExpr, elseExpr);
  }
}
