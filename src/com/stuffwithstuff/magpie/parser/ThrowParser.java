package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * Parser for a throw expression. Simply translates from:
 * 
 *     throw someExpression
 * 
 * to:
 * 
 *     someExpression *throw*()
 * 
 * Since there is a *throw*() method that throws the receiver, that does the right
 * thing.
 */
public class ThrowParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Expr body = parser.parseEndBlock();
    return Expr.call(token.getPosition(), body, "*throw*", Expr.nothing());
  }
}
