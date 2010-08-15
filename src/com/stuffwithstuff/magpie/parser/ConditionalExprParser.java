package com.stuffwithstuff.magpie.parser;

import java.util.Stack;

import com.stuffwithstuff.magpie.ast.Condition;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;

public class ConditionalExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.current().getPosition();
    
    // If expressions have their condition wrapped in a truthiness test. This
    // lets you use arbitrary types in conditions and let the types themselves
    // determine what true means for them. They get desugared like this:
    //
    // if foo then
    //
    // To:
    //
    // if foo true? then
    //
    // Let expressions and multiple if conditions get desugared like this:
    //
    // let a = foo
    // let b = bar
    // if c = d then
    //     e
    // end
    //
    // To:
    //
    // var a__ = foo
    // if a__ != () then
    //     var a = a__ // plus some type annotation to remove ()
    //     var b__ = bar
    //     if b__ != () then
    //         var b = b__
    //         if c = d then
    //             e
    //         end
    //     end
    // end
    //
    
    // Parse the conditions.
    Stack<Condition> conditions = new Stack<Condition>();
    while (true) {
      if (parser.match(TokenType.IF)) {
        Expr condition = parser.parseIfBlock();
        conditions.add(new Condition(condition));
      } else if (parser.match(TokenType.LET)) {
        // TODO(bob): Eventually allow tuple decomposition here.
        String name = parser.consume(TokenType.NAME).getString();
        parser.consume(TokenType.EQUALS);
        conditions.add(new Condition(name, parser.parseIfBlock()));
      } else {
        break;
      }
    }
    
    // Parse the then body.
    parser.consume(TokenType.THEN);
    Expr thenExpr = parser.parseThenBlock();
    
    // Parse the else body.
    Expr elseExpr = null;
    if (parser.match(TokenType.ELSE) || parser.match(TokenType.LINE, TokenType.ELSE)) {
      elseExpr = parser.parseElseBlock();
    } else {
      elseExpr = new NothingExpr(parser.last(1).getPosition());
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        conditions, thenExpr, elseExpr);
  }
}
