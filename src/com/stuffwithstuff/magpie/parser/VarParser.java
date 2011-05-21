package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class VarParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    boolean isMutable = token.getText().equals("var");
    
    PositionSpan span = parser.span();
    Pattern pattern = PatternParser.parse(parser);
    parser.consume(TokenType.EQUALS);
    Expr value = parser.parseExpressionOrBlock();
    
    return Expr.var(span.end(), isMutable, pattern, value);
  }
}
