package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class FnParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    
    // Parse the pattern if present.
    Pattern pattern = null;
    if (parser.lookAheadAny(TokenType.LEFT_PAREN)) {
      pattern = parser.parseFunctionType();
    } else {
      pattern = Pattern.wildcard();
    }
    
    // TODO(bob): Parse doc.
    
    // Parse the body.
    Expr expr = parser.parseExpressionOrBlock();
    
    return Expr.fn(span.end(), "", pattern, expr);
  }
}
