package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;

public class ConditionalExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.consume().getPosition();
    
    // Parse the condition.
    String name = null;
    Expr condition;
    if (parser.last(1).getType() == TokenType.IF) {
      condition = parser.parseBlock(TokenType.THEN);
    } else {
      // TODO(bob): Eventually allow tuple decomposition here.
      name = parser.consume(TokenType.NAME).getString();
      
      // See if there is an expression for the let condition.
      if (parser.lookAhead(TokenType.THEN)) {
        // let a then --> let a = a then
        condition = Expr.name(name);
      } else {
        parser.consume(TokenType.EQUALS);
        condition = parser.parseBlock(TokenType.THEN);
      }
    }
    
    // Parse the then body.
    parser.consume(TokenType.THEN);
    Expr thenExpr;
    boolean allowElse = true;
    if (parser.match(TokenType.LINE)){
      Position position = parser.last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parser.parseExpression());
        parser.consume(TokenType.LINE);
      } while (!parser.lookAhead(TokenType.ELSE) && !parser.match(TokenType.END));
      
      // Can't have an else clause if the then clause ended with "end".
      if (parser.last(1).getType() == TokenType.END) {
        allowElse = false;
      }
      
      position = position.union(parser.last(1).getPosition());
      thenExpr = new BlockExpr(position, exprs, true);
    } else {
      thenExpr = parser.parseExpression();
    }

    // Parse the else body.
    Expr elseExpr = null;
    if (allowElse && (parser.match(TokenType.ELSE))) {
      elseExpr = parser.parseBlock();
    } else {
      elseExpr = new NothingExpr(parser.last(1).getPosition());
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        name, condition, thenExpr, elseExpr);
  }
}
