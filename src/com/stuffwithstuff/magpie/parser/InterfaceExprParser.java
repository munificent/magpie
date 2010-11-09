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
    if (!isExtend) {
      // var Foo = Interface new("Foo")
      exprs.add(new VariableExpr(position, name,
          Expr.message(Expr.name("Interface"), Identifiers.NEW, Expr.string(name))));
    }
    
    // Parse the body.
    while (!parser.match(TokenType.END)) {
      // An interface contains declarations of methods, getters, and setters:
      //
      //    interface Foo
      //        def someMethod(a Int -> Int)
      //        get someGetter Int
      //        set someSetter Int
      //    end
      //
      // They get desugared to:
      //
      //    Foo declareMethod("someMethod", fn Function(Int, Int))
      //    Foo declareGetter("someGetter", fn Int)
      //    Foo declareSetter("someSetter", fn Int)
      
      // Parse the declaration keyword.
      TokenType memberType = parser.consumeAny(
          TokenType.DEF, TokenType.GET, TokenType.SET).getType();
      
      // Parse the name.
      String member = parser.consumeAny(
          TokenType.NAME, TokenType.OPERATOR).getString();
      
      switch (memberType) {
      case DEF:
        
        FunctionType function = parser.parseFunctionType();
        Expr methodType = Expr.message(Expr.name("Function"),
            Identifiers.NEW_TYPE, Expr.tuple(function.getParamType(),
                function.getReturnType(), Expr.bool(false)));
        exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_METHOD,
            Expr.tuple(Expr.string(member), Expr.fn(methodType))));
        break;
        
      case GET:
        Expr getterType = parser.parseTypeExpression();
        exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_GETTER,
            Expr.tuple(Expr.string(member), Expr.fn(getterType))));
        break;
        
      case SET:
        Expr setterType = parser.parseTypeExpression();
        exprs.add(Expr.message(Expr.name(name), Identifiers.DECLARE_SETTER,
            Expr.tuple(Expr.string(member), Expr.fn(setterType))));
        break;
      }

      parser.consume(TokenType.LINE);
    }
    
    return new BlockExpr(position, exprs);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.INTERFACE);
    return parseInterface(parser, false);
  }
}
