package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ImportParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    String scheme = null;
    if (parser.match(TokenType.FIELD)) {
      scheme = parser.last(1).getString();
    }
    
    // Parse the module name.
    String module = parser.consume(TokenType.NAME).getString();
    
    // Parse the name, if any.
    String name = null;
    if (parser.match(TokenType.NAME)) {
      name = parser.last(1).getString();
    }
    
    // Parse the rename, if any.
    String rename = null;
    if (parser.match(TokenType.EQUALS)) {
      rename = parser.consume(TokenType.NAME).getString();
    }
    
    return Expr.import_(span.end(), scheme, module, name, rename);
  }
}
