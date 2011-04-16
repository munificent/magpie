package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ImportParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    // Allow a leading . for a relative path.
    String name = "";
    if (parser.match(TokenType.DOT)) {
      name = ".";
    }
    
    name += parser.parseName().getString();
    
    return Expr.import_(span.end(), name);
  }
}
