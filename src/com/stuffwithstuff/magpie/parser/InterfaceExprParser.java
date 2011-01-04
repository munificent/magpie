package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Name;

public class InterfaceExprParser implements ExprParser {
  public static Expr parseInterface(MagpieParser parser, boolean isExtend) {
    String name = parser.consume(TokenType.NAME).getString();
    Position position = parser.last(1).getPosition();
    
    // See if it's generic.
    List<String> typeParams = new ArrayList<String>();
    if (parser.match(TokenType.LEFT_BRACKET)) {
      do {
        typeParams.add(parser.consume(TokenType.NAME).getString());
      } while (parser.match(TokenType.COMMA));
      parser.consume(TokenType.RIGHT_BRACKET);
    }
    
    parser.consume(TokenType.LINE);
    
    List<Expr> exprs = new ArrayList<Expr>();

    // Declare the interface:
    if (!isExtend) {
      if (typeParams.size() == 0) {
        // var Foo = Interface new("Foo")
        exprs.add(Expr.var(position, name,
            Expr.message(Expr.name("Interface"), Name.NEW, Expr.string(name))));
      } else {
        // var Foo = GenericInterface new("Foo", Array of("A", "B"))
        Expr[] paramExprs = new Expr[typeParams.size()];
        for (int i = 0; i < typeParams.size(); i++) {
          paramExprs[i] = Expr.string(typeParams.get(i));
        }
        
        Expr paramArray = Expr.message(Expr.name("Array"), "of",
            Expr.tuple(paramExprs));
        
        exprs.add(Expr.var(position, name,
            Expr.message(Expr.name("GenericInterface"), Name.NEW,
                Expr.tuple(Expr.string(name), paramArray))));
      }
    }
    
    // TODO(bob): If we are extending an interface, and it's a generic one, we
    // should check that we're extending it with the same number of type
    // arguments as the original definition. This would be bad:
    //
    // interface IFoo[A, B]
    //    ...
    // end
    //
    // extend interface IFoo[C]
    //    ...
    // end
    
    // Parse the body.
    while (!parser.match("end")) {
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
      String memberType;
      // TODO(bob): Hack temp. Will go away when this is moved into Magpie.
      if (parser.match("get")) {
        memberType = "get";
      } else if (parser.match("def")) {
        memberType = "def";
      } else {
        parser.consume(TokenType.SET);
        memberType = "set";
      }
      
      // Parse the name.
      String member = parser.consumeAny(
          TokenType.NAME, TokenType.OPERATOR).getString();
      
      if (memberType.equals("def")) {
        FunctionType function = parser.parseFunctionType();
        Expr methodType = Expr.message(Expr.name("Function"),
            Name.NEW_TYPE, Expr.tuple(function.getParamType(),
                function.getReturnType(), Expr.bool(false)));
        exprs.add(Expr.message(Expr.name(name), Name.DECLARE_METHOD,
            Expr.tuple(Expr.string(member),
                makeTypeFunction(typeParams, methodType))));
      } else if (memberType.equals("get")) {
        Expr getterType = parser.parseTypeExpression();
        exprs.add(Expr.message(Expr.name(name), Name.DECLARE_GETTER,
            Expr.tuple(Expr.string(member),
                makeTypeFunction(typeParams, getterType))));
      } else if (memberType.equals("set")) {
        Expr setterType = parser.parseTypeExpression();
        exprs.add(Expr.message(Expr.name(name), Name.DECLARE_SETTER,
            Expr.tuple(Expr.string(member),
                makeTypeFunction(typeParams, setterType))));
      }

      parser.consume(TokenType.LINE);
    }
    
    return Expr.block(exprs);
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.INTERFACE);
    return parseInterface(parser, false);
  }
  
  private static Expr makeTypeFunction(List<String> typeParams, Expr type) {
    Expr paramType;
    
    if (typeParams.size() == 0) {
      paramType = Expr.nothing();
    } else {
      Expr[] paramExprs = new Expr[typeParams.size()];
      for (int i = 0; i < typeParams.size(); i++) {
        paramExprs[i] = Expr.name("Type");
      }
      paramType = Expr.tuple(paramExprs);
    }
    
    FunctionType functionType = new FunctionType(typeParams, paramType,
        Expr.name("Type"), false);
    return Expr.fn(Position.none(), functionType, type);
  }
}
