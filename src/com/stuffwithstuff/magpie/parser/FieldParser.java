package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public class FieldParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    
    while (true) {
      String name = token.getString();
      Expr value = parser.parseExpression(20);
      fields.add(new Pair<String, Expr>(name, value));
      
      if (!parser.match(TokenType.COMMA)) break;
      token = parser.consume(TokenType.FIELD);
    }
    
    return Expr.record(position, fields);
  }
}
