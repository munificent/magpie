package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

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
    List<Pattern> patterns = new ArrayList<Pattern>();
    while (parser.match(TokenType.CASE)) {
      patterns.add(parsePattern(parser));
    }
    
    // Parse the else case, if present.
    Expr elseCase;
    if (parser.match(TokenType.ELSE)) {
      elseCase = parseBody(parser);
    } else {
      elseCase = new NothingExpr(position);
    }
    
    parser.consume(TokenType.END);
    
    // Desugar the cases to a series of chained if/thens.
    Expr chained = elseCase;
    for (int i = patterns.size() - 1; i >= 0; i--) {
      Pattern pattern = patterns.get(i);
      Position casePos = pattern.pattern.getPosition().union(
          pattern.body.getPosition());
      
      Expr condition = Expr.message(Expr.name("__value__"), "==",
          pattern.pattern);
      
      Expr body = pattern.body;
      
      // Bind a name if there is one.
      if (pattern.binding != null) {
        List<Expr> bodyExprs = new ArrayList<Expr>();
        bodyExprs.add(new VariableExpr(body.getPosition(), pattern.binding,
            Expr.name("__value__")));
        bodyExprs.add(body);
        body = new BlockExpr(body.getPosition(), bodyExprs);
      }
      
      chained = new IfExpr(casePos, null, condition, body, chained);
    }

    exprs.add(chained);
    
    position = position.union(parser.last(1).getPosition());
    return new BlockExpr(position, exprs);
  }
  
  private Pattern parsePattern(MagpieParser parser) {
    String name = parseBinding(parser);
    Expr pattern = parser.parseExpression();

    parser.consume(TokenType.THEN);
    Expr body = parseBody(parser);

    return new Pattern(name, pattern, body);
  }
  
  private String parseBinding(MagpieParser parser) {
    if ((parser.current().getType() == TokenType.NAME) &&
        Character.isLowerCase(parser.current().getString().charAt(0))) {
      return parser.consume().getString();
    }
    
    // The token isn't a valid variable binding name.
    return null;
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
  private Expr parseBody(MagpieParser parser) {
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
  
  private static class Pattern {
    public Pattern(String binding, Expr pattern, Expr body) {
      this.binding = binding;
      this.pattern = pattern;
      this.body = body;
    }
    
    public final String binding;
    public final Expr pattern;
    public final Expr body;
  }
}
