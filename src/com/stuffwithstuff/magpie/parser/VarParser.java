package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class VarParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    Pattern pattern = PatternParser.parse(parser);
    parser.consume("=");
    Expr value = parser.parseEndBlock();
    
    position = position.union(parser.last(1).getPosition());
    return Expr.var(position, pattern, value);
  }
}
