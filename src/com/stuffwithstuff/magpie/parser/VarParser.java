package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class VarParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    Pattern pattern = PatternParser.parse(parser);
    parser.consume(TokenType.EQUALS);
    Expr value = parser.parseEndBlock();
    
    return Expr.var(span.end(), pattern, value);
  }
}
