package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.util.Pair;

public class LetExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.consume(TokenType.LET).getPosition();
    
    // TODO(bob): Eventually allow tuple decomposition here.
    String name = parser.consume(TokenType.NAME).getString();
    
    // See if there is an expression for the let condition.
    Expr condition;
    if (parser.lookAhead("then")) {
      // let a then --> let a = a then
      condition = Expr.name(name);
    } else {
      parser.consume(TokenType.EQUALS);
      condition = parser.parseBlock("then").getKey();
    }
    
    // Parse the then body.
    parser.consume("then");
    Pair<Expr, Token> thenParse = parser.parseBlock("else", "end");
    Expr thenExpr = thenParse.getKey();

    // Parse the else body.
    boolean consumedEnd = (thenParse.getValue() != null) &&
        (thenParse.getValue().isKeyword("end"));
    
    Expr elseExpr;
    if (!consumedEnd && parser.match("else")) {
      elseExpr = parser.parseEndBlock();
    } else {
      elseExpr = Expr.nothing(parser.last(1).getPosition());
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        name, condition, thenExpr, elseExpr);
  }
}
