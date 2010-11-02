package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;
import com.stuffwithstuff.magpie.util.Pair;

// TODO(bob): This whole implementation is pretty hideous. Just slapping
// something together so I can start getting it working. Will refactor and clean
// up once it does stuff and there's a spec.
public class MatchExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.MATCH);
    Position position = parser.last(1).getPosition();
    
    List<Expr> exprs = new ArrayList<Expr>();
    
    // Parse the value.
    Expr value = parser.parseExpression();
    // TODO(bob): Need to make sure name is unique, and put it scope local to
    // match expr.
    exprs.add(new VariableExpr(value.getPosition(), "__value__", value));
    
    // Require a newline between the value and the first case.
    parser.consume(TokenType.LINE);
        
    // Parse the cases.
    List<Pair<Expr, Expr>> cases = new ArrayList<Pair<Expr, Expr>>();
    while (parser.match(TokenType.CASE)) {
      // TODO(bob): Temp! Should do pattern parsing.
      Expr pattern = parser.parseExpression();
      parser.consume(TokenType.THEN);
      Expr body = parseCase(parser);
      
      cases.add(new Pair<Expr, Expr>(pattern, body));
    }
    
    // Parse the else case, if present.
    Expr elseCase;
    if (parser.match(TokenType.ELSE)) {
      elseCase = parseCase(parser);
    } else {
      elseCase = new NothingExpr(position);
    }
    
    parser.consume(TokenType.END);
    
    // Desugar the cases to a series of chained if/thens.
    Expr chained = elseCase;
    for (int i = cases.size() - 1; i >= 0; i--) {
      Pair<Expr, Expr> thisCase = cases.get(i);
      Position casePos = thisCase.getKey().getPosition().union(
          thisCase.getValue().getPosition());
      
      Expr condition = Expr.message(Expr.name("__value__"), "==", thisCase.getKey());
      
      chained = new IfExpr(casePos, null, condition, thisCase.getValue(),
          chained);
    }

    exprs.add(chained);
    
    position = position.union(parser.last(1).getPosition());
    return new BlockExpr(position, exprs);
  }
  
  /**
   * Parses the body of a single case: the expression after "then" or "else".
   * Handles both single expression and block bodies. Will leave the parser
   * sitting on the "case", "else", or "end" token that indicates the end of
   * this case.
   * 
   * @param   parser  The parser.
   * @return          The parsed case body.
   */
  private Expr parseCase(MagpieParser parser) {
    if (parser.match(TokenType.LINE)){
      Position position = parser.last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      while (!parser.lookAheadAny(TokenType.CASE, TokenType.ELSE,
          TokenType.END)) {
        exprs.add(parser.parseExpression());
        parser.consume(TokenType.LINE);
      }
      
      position = position.union(parser.last(1).getPosition());
      return new BlockExpr(position, exprs);
    } else {
      Expr body = parser.parseExpression();
      parser.consume(TokenType.LINE);
      return body;
    }
  }
}
