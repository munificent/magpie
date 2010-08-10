package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.LoopExpr;
import com.stuffwithstuff.magpie.ast.MessageExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;

public class LoopExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    // "while" and "for" loop.
    Position startPos = parser.current().getPosition();
    
    // A for loop is desugared from this:
    //
    //   for a <- foo do
    //     print a
    //   end
    //
    // To:
    //
    //   do
    //     def __a_gen = foo.generate
    //     while __a_gen.next do
    //       def a = __a_gen.current
    //       print a
    //     end
    //   end
    
    List<Expr> generators = new ArrayList<Expr>();
    List<Expr> initializers = new ArrayList<Expr>();
    
    List<Expr> conditions = new ArrayList<Expr>();
    
    while (parser.match(TokenType.WHILE) || parser.match(TokenType.FOR)) {
      if (parser.last(1).getType() == TokenType.WHILE) {
        conditions.add(parser.parseExpression());
      } else {
        Token nameToken = parser.consume(TokenType.NAME);
        String variable = nameToken.getString();
        Position position = nameToken.getPosition();
        parser.consume(TokenType.EQUALS);
        Expr generator = parser.parseExpression();
        
        // Initialize the generator before the loop.
        String generatorVar = variable + " gen";
        generators.add(new VariableExpr(position, generatorVar,
            new MessageExpr(Position.none(), generator, "iterate", new NothingExpr(position))));
        
        // The the condition expression just increments the generator.
        conditions.add(new MessageExpr(Position.none(), 
            Expr.name(generatorVar), "next", new NothingExpr(position)));
        
        // In the body of the loop, we need to initialize the variable.
        initializers.add(new VariableExpr(position, variable,
            new MessageExpr(Position.none(), Expr.name(generatorVar), "current", new NothingExpr(position))));
      }
      parser.match(TokenType.LINE); // Optional line after a clause.
    }
    
    parser.consume(TokenType.DO);
    Expr body = parser.parseBlock();
    
    // If there are "for" loops, mix in the generators and variables.
    if (generators.size() > 0) {
      // Create the variables inside the loop.
      List<Expr> innerBlock = new ArrayList<Expr>();
      for (Expr initializer : initializers) innerBlock.add(initializer);

      // Then execute the main body.
      innerBlock.add(body);
      body = new BlockExpr(innerBlock);
      
      // Create the generators outside the loop.
      List<Expr> outerBlock = new ArrayList<Expr>();
      for (Expr generator : generators) outerBlock.add(generator);
      
      // Then execute the loop.
      outerBlock.add(new LoopExpr(Position.union(startPos, body.getPosition()), conditions, body));
      return new BlockExpr(outerBlock);
    }
    
    return new LoopExpr(Position.union(startPos, body.getPosition()), conditions, body);
  }
}
