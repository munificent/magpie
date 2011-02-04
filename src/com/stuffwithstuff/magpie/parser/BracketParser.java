package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;

public class BracketParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // A call with type arguments.
    List<Expr> typeArgs = new ArrayList<Expr>();
    do {
      typeArgs.add(TypeParser.parse(parser));
    } while(parser.match(TokenType.COMMA));
    parser.consume(TokenType.RIGHT_BRACKET);
    
    // See if there is a regular argument too.
    Expr arg;
    if (parser.match(TokenType.LEFT_PAREN)) {
      arg = parser.parseExpression();
      parser.consume(TokenType.RIGHT_PAREN);
    } else {
      arg = Expr.nothing();
    }
    
    return Expr.call(left, typeArgs, arg);
  }
  
  @Override
  public int getStickiness() { return 100; }
}
