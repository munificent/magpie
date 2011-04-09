package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public class FieldParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    Set<String> usedNames = new HashSet<String>();
    
    while (true) {
      String name = token.getString();
      Expr value = parser.parseExpression(Precedence.COMPOSITION);
      fields.add(new Pair<String, Expr>(name, value));
      
      if (usedNames.contains(name)) {
        throw new ParseException("A record may only use a field name once.");
      }
      usedNames.add(name);
      
      if (!parser.match(TokenType.COMMA)) break;
      token = parser.consume(TokenType.FIELD);
    }
    
    return Expr.record(position, fields);
  }
}
