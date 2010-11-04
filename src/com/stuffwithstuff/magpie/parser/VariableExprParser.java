package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class VariableExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position position = parser.consume(TokenType.VAR).getPosition();
    
    // TODO(bob): support multiple definitions and tuple decomposition here
    String name = parser.consume(TokenType.NAME).getString();
    parser.consume(TokenType.EQUALS);
    Expr value = parser.parseBlock();
    
    position = position.union(value.getPosition());
    
    return new VariableExpr(position, name, value);
  }
}
