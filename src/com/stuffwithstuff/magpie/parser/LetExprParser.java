package com.stuffwithstuff.magpie.parser;

import java.util.EnumSet;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.parser.MagpieParser.BlockOptions;
import com.stuffwithstuff.magpie.util.Ref;

public class LetExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.consume(TokenType.LET).getPosition();
    
    // TODO(bob): Eventually allow tuple decomposition here.
    String name = parser.consume(TokenType.NAME).getString();
    
    // See if there is an expression for the let condition.
    Expr condition;
    if (parser.lookAhead(TokenType.THEN)) {
      // let a then --> let a = a then
      condition = Expr.name(name);
    } else {
      parser.consume(TokenType.EQUALS);
      condition = parser.parseBlock(EnumSet.noneOf(BlockOptions.class),
          TokenType.THEN);
    }
    
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
        name, condition, thenExpr, elseExpr);
  }
}
