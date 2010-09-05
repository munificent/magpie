package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.MessageExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.StringExpr;
import com.stuffwithstuff.magpie.ast.TupleExpr;

public class DefineExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // An expression like:
    //
    //    def Foo bar bang(a) print(a)
    //
    // Is desugared to:
    //
    //    Foo bar defineMethod("bang", fn(a) print(a))
    
    Position startPos = parser.consumeAny(
        TokenType.DEF, TokenType.SHARED).getPosition();
    
    boolean isShared = parser.last(1).getType() == TokenType.SHARED;
    
    // Parse the target we're defining the method on and the method name.
    // TODO(bob): This is a total hack. It shouldn't just allow an arbitrary
    // stream of names and operators. It needs to basically parse a message send
    // followed by a single name or operator, followed by the type signature.
    List<Token> names = new ArrayList<Token>();
    while (parser.matchAny(TokenType.NAME, TokenType.OPERATOR)) {
      names.add(parser.last(1));
    }
    
    if (names.size() < 0) throw new ParseException(
        "Could not find a method name to define at " + startPos + ".");
    
    // The last identifier is the method name.
    Token method = names.get(names.size() - 1);
    
    // The others form an expression.
    Expr message = new MessageExpr(names.get(0).getPosition(), null,
        names.get(0).getString(), null);
    for (int i = 1; i < names.size() - 1; i++) {
      message = new MessageExpr(names.get(i).getPosition(), message,
          names.get(i).getString(), null);
    }
    
    Expr function = parser.parseFunction();
    Expr arg = new TupleExpr(new StringExpr(method), function);
    
    String addMethod = isShared ? "defineSharedMethod" : "defineMethod";
    return new MessageExpr(startPos, message, addMethod, arg);
  }
}
