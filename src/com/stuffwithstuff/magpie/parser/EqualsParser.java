package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class EqualsParser implements InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the value being assigned.
    Expr value = parser.parsePrecedence(getPrecedence() - 1);
    return ConvertAssignmentExpr.convert(left, value);
  }
  
  @Override
  public int getPrecedence() { return Precedence.ASSIGNMENT; }
}
