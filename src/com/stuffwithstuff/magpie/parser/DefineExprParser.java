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
    
    // Allow a sequence of identifiers like:
    // def a b c d e()
    // Where the last one will be the method name, and the other ones will form
    // an expression that evaluates to the class to define the method on.
    // TODO(bob): Ideally should allow an arbitrary expression here.
    // TODO(bob): Also need to allow defining an operator here.
    List<Token> names = new ArrayList<Token>();
    while (parser.match(TokenType.NAME)) {
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
