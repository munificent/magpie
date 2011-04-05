package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * Parses an indexer call like "list[index]".
 */
public class BracketInfixParser implements InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the argument, if any.
    Expr arg = parser.groupExpression(TokenType.RIGHT_BRACKET);
    return Expr.message(token.getPosition(), left, "[]", arg);
  }
  
  @Override
  public int getStickiness() { return Precedence.MESSAGE; }
}
