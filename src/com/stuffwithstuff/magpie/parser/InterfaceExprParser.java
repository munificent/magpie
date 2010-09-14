package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class InterfaceExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.INTERFACE);
    
    String name = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    parser.consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();

    // Declare the interface:
    // var Foo = Interface new("Foo")
    exprs.add(new VariableExpr(position, name,
        Expr.message(Expr.name("Interface"), Identifiers.NEW, Expr.string(name))));
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
      // An interface contains method declarations: i.e. names and signatures
      // with no body:
      //
      //    foo(a Int -> Int)
      //
      // They get desugared to:
      //
      //    Foo declareMethod("foo", (fn() Int), (fn() Int))
      
      String method = parser.consumeAny(TokenType.NAME, TokenType.OPERATOR).getString();
      FunctionType type = parser.parseFunctionType();

      exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_METHOD,
          Expr.tuple(Expr.string(method),
              Expr.fn(type.getParamType()),
              Expr.fn(type.getReturnType()))));
      
      parser.consume(TokenType.LINE);
    }
    
    return new BlockExpr(position, exprs, false);
  }
}
