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
    Expr valueExpr = Expr.name("__value__");
    
    // Require a newline between the value and the first case.
    parser.consume(TokenType.LINE);
        
    // Parse the cases.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    while (parser.match(TokenType.CASE)) {
      cases.add(parseCase(parser));
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
    for (int i = cases.size() - 1; i >= 0; i--) {
      MatchCase thisCase = cases.get(i);

      Expr condition = thisCase.pattern.createPredicate(valueExpr);
      Expr body = thisCase.body;
      
      // Bind a name if there is one.
      if (thisCase.binding != null) {
        List<Expr> bodyExprs = new ArrayList<Expr>();
        bodyExprs.add(new VariableExpr(body.getPosition(), thisCase.binding,
            valueExpr));
        bodyExprs.add(body);
        body = new BlockExpr(body.getPosition(), bodyExprs);
      }
      
      chained = new IfExpr(body.getPosition(), null, condition, body, chained);
    }

    exprs.add(chained);
    
    position = position.union(parser.last(1).getPosition());
    return new BlockExpr(position, exprs);
  }
  
  private MatchCase parseCase(MagpieParser parser) {
    // Valid patterns:
    // 1
    // a 1
    // b true
    // c
    // d Int
    // e Int | String
    // f (Int, String)
    // g (Int => String) => Bool
    
    String name = parseBinding(parser);
    Pattern pattern = parsePattern(parser);

    parser.consume(TokenType.THEN);
    Expr body = parseBody(parser);

    return new MatchCase(name, pattern, body);
  }
  
  private String parseBinding(MagpieParser parser) {
    if ((parser.current().getType() == TokenType.NAME) &&
        Character.isLowerCase(parser.current().getString().charAt(0))) {
      return parser.consume().getString();
    }
    
    // The token isn't a valid variable binding name.
    return null;
  }
  
  private Pattern parsePattern(MagpieParser parser) {
    if (parser.match(TokenType.BOOL)) {
      return new LiteralPattern(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      return new LiteralPattern(Expr.integer(parser.last(1).getInt()));
    } else if (parser.match(TokenType.STRING)) {
      return new LiteralPattern(Expr.string(parser.last(1).getString()));
    } else {
      Expr typeAnnotation = parser.parseTypeExpression();
      return new TypePattern(typeAnnotation);
    }
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
  
  private static class MatchCase {
    public MatchCase(String binding, Pattern pattern, Expr body) {
      this.binding = binding;
      this.pattern = pattern;
      this.body = body;
    }
    
    public final String binding;
    public final Pattern pattern;
    public final Expr body;
  }
  
  private interface Pattern {
    Expr createPredicate(Expr value);
  }
  
  private static class LiteralPattern implements Pattern {
    public LiteralPattern(Expr value) {
      mValue = value;
    }
    
    public Expr createPredicate(Expr value) {
      return Expr.message(value, "==", mValue);
    }

    private final Expr mValue;
  }
  
  private static class TypePattern implements Pattern {
    public TypePattern(Expr type) {
      mType = type;
    }
    
    public Expr createPredicate(Expr value) {
      return Expr.staticMessage(value, "is", mType);
    }
    
    private final Expr mType;
  }
}
