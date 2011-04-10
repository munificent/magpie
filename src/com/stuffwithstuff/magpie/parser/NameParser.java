package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.TupleExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

public class NameParser implements PrefixParser, InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return parse(parser, null, token);
  }
  
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the whole fully-qualified name.
    Token fullName = parser.parseName(true);
    
    // Parse the argument, if any.
    Expr arg = null;
    if (parser.match(TokenType.LEFT_PAREN)) {
      arg = parser.groupExpression(TokenType.RIGHT_PAREN);
    }
    
    // Pass the block, if any.
    if (parser.match("with")) {
      // Parse the parameter list if given.
      Pattern blockType;
      if (parser.lookAhead(TokenType.LEFT_PAREN)) {
        blockType = parser.parseFunctionType();
      } else {
        // Else just assume a single "it" parameter.
        blockType = Pattern.variable(Name.IT);
      }

      // Parse the block and wrap it in a function.
      Expr block = parser.parseExpressionOrBlock();
      block = Expr.fn(block.getPosition(), blockType, block);
      
      // Add it to the argument list.
      arg = addTupleField(arg, block);
    }
    
    // See if this is a bare name, or a method call.
    if ((left == null) && (arg == null)) {
      return Expr.variable(token.getPosition(), fullName.getString());
    } else {
      return Expr.call(fullName.getPosition(), left, fullName.getString(), arg);
    }
  }
  
  @Override
  public int getStickiness() { return Precedence.MESSAGE; }

  private Expr addTupleField(Expr expr, Expr field) {
    if (expr == null) {
      return field;
    } else if (expr instanceof NothingExpr) {
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
