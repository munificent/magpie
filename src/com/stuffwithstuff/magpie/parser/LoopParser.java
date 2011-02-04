package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Name;

public class LoopParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    // "while" and "for" loop.
    Position startPos = token.getPosition();
    
    // A loop is desugared from this:
    //
    //   while bar
    //   for a = foo do
    //       print(a)
    //   end
    //
    // To:
    //
    //   do
    //       // beforeLoop:
    //       var __a_gen = foo iterate()
    //       // end beforeLoop
    //       loop
    //           // eachLoop:
    //           if bar then nothing else break
    //           if __a_gen next() then nothing else break
    //           var a = __a_gen current
    //           // end eachLoop
    //           // body:
    //           print(a)
    //       end
    //   end
    
    List<Expr> beforeLoop = new ArrayList<Expr>();
    List<Expr> eachLoop = new ArrayList<Expr>();
    
    while (true) {
      if (token.isKeyword("while")) {
        Expr condition = parser.parseExpression();
        eachLoop.add(Expr.if_(condition,
            Expr.nothing(),
            Expr.break_(condition.getPosition())));
      } else {
        Token nameToken = parser.consume(TokenType.NAME);
        String variable = nameToken.getString();
        Position position = nameToken.getPosition();
        parser.consume(TokenType.EQUALS);
        Expr generator = parser.parseExpression();
        
        // Initialize the generator before the loop.
        String generatorVar = variable + " gen";
        beforeLoop.add(Expr.var(position, generatorVar,
            Expr.message(generator, Name.ITERATE, Expr.nothing(position))));
        
        // Each iteration, advance the iterator and break if done.
        eachLoop.add(Expr.if_(
            Expr.message(Expr.name(generatorVar), Name.NEXT, Expr.nothing(position)),
            Expr.nothing(),
            Expr.break_(position)));
        
        // If not done, create the loop variable.
        eachLoop.add(Expr.var(position, variable,
            Expr.message(Expr.name(generatorVar), Name.CURRENT)));
      }
      parser.match(TokenType.LINE); // Optional line after a clause.
      
      if (parser.match("while") || parser.match("for")) {
        token = parser.last(1);
      } else {
        break;
      }
    }
    
    parser.consume("do");
    Expr body = parser.parseEndBlock();

    Position position = startPos.union(body.getPosition());
    
    // Build the loop body.
    List<Expr> loopBlock = new ArrayList<Expr>();
    for (Expr expr : eachLoop) loopBlock.add(expr);

    // Then execute the main body.
    loopBlock.add(body);
    Expr loopBody = Expr.block(loopBlock);
    
    // Add the iterators outside of the loop.
    List<Expr> outerBlock = new ArrayList<Expr>();
    for (Expr expr : beforeLoop) outerBlock.add(expr);

    // Add the main loop.
    outerBlock.add(Expr.loop(position, loopBody));

    // Wrap the iterators in their own scope.
    return Expr.scope(Expr.block(outerBlock));
  }
}
