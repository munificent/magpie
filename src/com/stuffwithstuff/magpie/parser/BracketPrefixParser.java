package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;

public class BracketPrefixParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    
    List<Expr> elements = new ArrayList<Expr>();
    
    // Check for the empty list.
    if (!parser.lookAhead(TokenType.RIGHT_BRACKET)) {
      do {
      // Higher precedence than COMPOSITION so that "," are parsed by the list
      // and not as tuples.
      elements.add(parser.parsePrecedence(Precedence.LOGICAL));
      } while (parser.match(TokenType.COMMA));
    }
    
    parser.consume(TokenType.RIGHT_BRACKET);
    
    return Expr.array(span.end(), elements);
  }
}
