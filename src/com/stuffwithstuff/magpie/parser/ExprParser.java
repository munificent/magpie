package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public interface ExprParser {
  Expr parse(MagpieParser parser);
}
