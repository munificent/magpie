package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;

public class BraceParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    parser.pushQuote();
    
    Position position = token.getPosition();
    
    // Ignore a line after {
    parser.match(TokenType.LINE);
    
    // Parse a sequence of expressions.
    List<Expr> exprs = new ArrayList<Expr>();
    while (true) {
      exprs.add(parser.parseExpression());
      
      // Stop when we hit the closing }.
      if (parser.match(TokenType.RIGHT_BRACE)) break;
      if (parser.match(TokenType.LINE, TokenType.RIGHT_BRACE)) break;
      
      // Each expression is separated by a line.
      parser.consume(TokenType.LINE);
    }
    
    position = position.union(parser.last(1).getPosition());
    parser.popQuote();
    
    if (exprs.size() == 1) return Expr.quote(position, exprs.get(0));
    
    return Expr.quote(position, Expr.block(exprs));
  }
}
