package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

public class CommaParser implements InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // This gets called when the first "," so we have one field already and
    // we've consumed the first ",".
    int index = 0;
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    fields.add(new Pair<String, Expr>(Name.getTupleField(index++), left));
    
    // Parse the rest of the fields.
    do {
      String name;
      if (parser.match(TokenType.FIELD)) {
        name = parser.last(1).getString();
      } else {
        name = Name.getTupleField(index);
      }
      index++;
      
      fields.add(new Pair<String, Expr>(name, parser.parsePrecedence(getPrecedence())));
    } while (parser.match(TokenType.COMMA));
    
    return Expr.record(left.getPosition().union(parser.last(1).getPosition()), fields);
  }
  
  @Override
  public int getPrecedence() { return Precedence.RECORD; }
}
