package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.CallExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.TupleExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.interpreter.Name;

public class WithParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the parameter list if given.
    Pattern blockType;
    if (parser.lookAhead(TokenType.LEFT_PAREN)) {
      blockType = parser.parseFunctionType();
    } else {
      // Else just assume a single "it" parameter.
      blockType = new VariablePattern(Name.IT, null);
    }

    // Parse the block and wrap it in a function.
    Expr block = parser.parseEndBlock();
    block = Expr.fn(block.getPosition(), blockType, block);
    
    // Apply it to the previous expression.
    if (left instanceof CallExpr) {
      // foo(123) with ...  --> Call(Msg(foo), Tuple(123, block))
      CallExpr call = (CallExpr)left;
      Expr arg = addTupleField(call.getArg(), block);
      return Expr.call(call.getTarget(), arg);
    } else {
      // 123 with ...  --> Call(Int(123), block)
      return Expr.call(left, block);
    }
  }
  
  @Override
  public int getStickiness() { return 100; }

  private Expr addTupleField(Expr expr, Expr field) {
    if (expr instanceof NothingExpr) {
      return field;
    } else if (expr instanceof TupleExpr) {
      TupleExpr tuple = (TupleExpr)expr;
      List<Expr> fields = new ArrayList<Expr>(tuple.getFields());
      fields.add(field);
      return Expr.tuple(fields);
    } else {
      return Expr.tuple(expr, field);
    }
  }
}
