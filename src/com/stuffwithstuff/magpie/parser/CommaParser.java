package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;

public class CommaParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // This gets called when the first "," so we have one field already and
    // we've consumed the first ",".
    List<Expr> fields = new ArrayList<Expr>();
    fields.add(left);
    
    // Parse the rest of the fields.
    do {
      fields.add(parser.parseExpression(getStickiness()));
    } while (parser.match(TokenType.COMMA));
    
    return Expr.tuple(fields);
  }
  
  @Override
  public int getStickiness() { return 40; }
}
