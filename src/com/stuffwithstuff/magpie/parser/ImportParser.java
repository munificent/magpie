package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ImportParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    // Allow a leading . for a relative path.
    String module = "";
    if (parser.match(TokenType.DOT)) {
      module = ".";
    }
    
    // Parse the module name.
    module += parser.parseName().getString();
    
    // Parse the name, if any.
    String name = null;
    if (parser.lookAhead(TokenType.NAME)) {
      name = parser.parseName().getString();
    }
    
    // Parse the rename, if any.
    String rename = null;
    if (parser.match(TokenType.EQUALS)) {
      rename = parser.parseName().getString();
    }
    
    return Expr.import_(span.end(), module, name, rename);
  }
}
