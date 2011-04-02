package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class MessageParser implements PrefixParser, InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return parse(parser, null, token);
  }
  
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the whole fully-qualified name.
    Token fullName = parser.parseName(true);
    
    // Parse the argument, if any.
    Expr arg = null;
    if (parser.match(TokenType.LEFT_PAREN)) {
      // TODO(bob): Allow block here, like:
      // foo(
      //     a
      //     b
      // )
      arg = parser.groupExpression(TokenType.RIGHT_PAREN);
    }
    
    return Expr.message(fullName.getPosition(), left, fullName.getString(), arg);
  }
  
  @Override
  public int getStickiness() { return 100; }
}
