package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

// TODO(bob): Merge this code with CommaParser.
public class FieldParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    Set<String> usedNames = new HashSet<String>();
    
    int index = 1;
    String name = token.getString();
    Position namePosition = token.getPosition();
    while (true) {
      Expr value = parser.parseExpression(Precedence.RECORD);
      fields.add(new Pair<String, Expr>(name, value));
      
      if (usedNames.contains(name)) {
        throw new ParseException(namePosition,
            "A record may only use a field name once.");
      }
      usedNames.add(name);
      
      if (!parser.match(TokenType.COMMA)) break;
      
      if (parser.match(TokenType.FIELD)) {
        name = parser.last(1).getString();
        namePosition = parser.last(1).getPosition();
      } else {
        name = Name.getTupleField(index);
        namePosition = parser.current().getPosition();
      }
      index++;
    }
    
    return Expr.record(position, fields);
  }
}
