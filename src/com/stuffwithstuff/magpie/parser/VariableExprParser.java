package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.StaticFnExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.FunctionType;

public class VariableExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position position = parser.consume(TokenType.VAR).getPosition();
    
    // TODO(bob): support multiple definitions and tuple decomposition here
    String name = parser.consume(TokenType.NAME).getString();
    
    // TODO(bob): This should go away once everything is using def. var should
    // just be for non-function variables.
    
    // See if we're defining a static function in shorthand notation:
    // var foo[] blah
    List<String> staticParams = new ArrayList<String>();
    if (parser.match(TokenType.LEFT_BRACKET)) {
      while (true) {
        String staticParam = parser.consume(TokenType.NAME).getString();
        staticParams.add(staticParam);
        if (!parser.match(TokenType.COMMA)) break;
      }
      parser.consume(TokenType.RIGHT_BRACKET);
    }
    
    // See if we're defining a function in shorthand notation:
    // var foo() blah
    Expr value;
    if (parser.lookAhead(TokenType.LEFT_PAREN)) {
      Position fnPosition = parser.last(1).getPosition();
      
      FunctionType type = parser.parseFunctionType();
      Expr body = parser.parseBlock();
      
      // Desugar it to: var foo = fn () blah
      value = new FnExpr(fnPosition.union(body.getPosition()), type, body);
    } else {
      // Just a regular variable definition.
      parser.consume(TokenType.EQUALS);
      
      value = parser.parseBlock();
    }
    
    position = position.union(value.getPosition());
    
    // Wrap it in a static function if we have static parameters.
    if (staticParams.size() > 0) {
      value = new StaticFnExpr(position, staticParams, value);
    }
    
    return new VariableExpr(position, name, value);
  }
}
