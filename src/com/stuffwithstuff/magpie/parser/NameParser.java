package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.RecordExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

public class NameParser implements PrefixParser, InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return parse(parser, null, token);
  }
  
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
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
      // TODO(bob): Parse doc comment.
      block = Expr.fn(block.getPosition(), "", blockType, block);
      
      // Add it to the argument list.
      arg = appendField(arg, block);
    }
    
    // See if this is a bare name, or a method call.
    if (left == null) {
      if (arg == null) {
        return Expr.name(token.getPosition(), token.getString());
      } else {
        return Expr.call(token.getPosition(), Expr.nothing(), token.getString(), arg);
      }
    } else {
      if (arg == null) {
        return Expr.call(token.getPosition(), left, token.getString());
      } else {
        return Expr.call(token.getPosition(), left, token.getString(), arg);
      }
    }
  }
  
  @Override
  public int getPrecedence() { return Precedence.MESSAGE; }

  private Expr appendField(Expr expr, Expr field) {
    if (expr == null) {
      return field;
    } else if (expr instanceof NothingExpr) {
      return field;
    } else if (expr instanceof RecordExpr) {
      RecordExpr record = (RecordExpr)expr;
      List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>(record.getFields());
      fields.add(new Pair<String, Expr>(Name.getTupleField(fields.size()), field));
      return Expr.record(record.getPosition(), fields);
    } else {
      return Expr.record(expr, field);
    }
  }
}
