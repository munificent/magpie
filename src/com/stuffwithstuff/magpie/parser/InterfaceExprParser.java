package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.VariableExpr;

public class InterfaceExprParser implements ExprParser {
  public static Expr parseInterface(MagpieParser parser, boolean isExtend) {
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
      // An interface contains member declarations: i.e. names and signatures
      // with no body:
      //
      //    foo(a Int -> Int)
      //    bar Int
      //
      // They get desugared to:
      //
      //    Foo declareMember("foo", fn() Function(Int, Int))
      //    Foo declareMember("bar", fn() Int)
      
      String member = parser.consumeAny(
          TokenType.NAME, TokenType.OPERATOR).getString();
      
      // Parse the member's type.
      Expr type;
      if (parser.lookAhead(TokenType.LEFT_PAREN)) {
        // It's a method.
        FunctionType function = parser.parseFunctionType();
        type = Expr.message(Expr.name("Function"), Identifiers.CALL,
            Expr.tuple(function.getParamType(), function.getReturnType()));
      } else {
        // It's a getter.
        type = parser.parseTypeExpression();
      }
      
      exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_MEMBER,
          Expr.tuple(Expr.string(member), Expr.fn(type))));
      
      parser.consume(TokenType.LINE);
    }
    
    return new BlockExpr(position, exprs, false);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.INTERFACE);
    return parseInterface(parser, false);
  }
}
