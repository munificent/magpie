package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.pattern.LiteralPattern;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.util.Pair;

// TODO(bob): This whole implementation is pretty hideous. Just slapping
// something together so I can start getting it working. Will refactor and clean
// up once it does stuff and there's a spec.
public class MatchExprParser implements ExprParser {
  /**
   * Converts a series of match cases down to a primitive if/then expression
   * that can be directly evaluated. Also inserts any bindings done by the
   * cases.
   * 
   * @param valueExpr  An expression that evaluates to the value being matched.
   * @param cases      The list of match cases.
   * @param elseExpr   The final else case or null if there is none.
   * @return           An expression that will evaluate the cases.
   */
  public static Expr desugarCases(Expr valueExpr, List<MatchCase> cases,
      Expr elseExpr) {
    if (elseExpr == null) elseExpr = Expr.nothing();
    
    // Desugar the cases to a series of chained if/thens.
    Expr chained = elseExpr;
    for (int i = cases.size() - 1; i >= 0; i--) {
      MatchCase thisCase = cases.get(i);

      Expr condition = thisCase.getPattern().createPredicate(valueExpr);
      Expr body = thisCase.getBody();
      
      // Bind a name if there is one.
      if (thisCase.hasBinding()) {
        List<Expr> bodyExprs = new ArrayList<Expr>();
        bodyExprs.add(Expr.var(body.getPosition(),
            thisCase.getBinding(), valueExpr));
        bodyExprs.add(body);
        body = Expr.block(bodyExprs);
      }
      
      chained = new IfExpr(body.getPosition(), null, condition, body, chained);
    }

    return chained;
  }
  
  private static MatchCase parseCase(MagpieParser parser) {
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

    parser.consume("then");
    
    Pair<Expr, Token> bodyParse = parser.parseBlock("else", "end",
        TokenType.CASE);
    
    // Allow newlines to separate single-line case and else cases.
    if ((bodyParse.getValue() == null) &&
        (parser.lookAhead(TokenType.LINE, TokenType.CASE) ||
         parser.lookAhead(TokenType.LINE, "else"))) {
      parser.consume(TokenType.LINE);
    }
    
    return new MatchCase(name, pattern, bodyParse.getKey());
  }
  
  public static String parseBinding(MagpieParser parser) {
    if ((parser.current().getType() == TokenType.NAME) &&
        Character.isLowerCase(parser.current().getString().charAt(0))) {
      return parser.consume().getString();
    }
    
    // The token isn't a valid variable binding name.
    return null;
  }
  
  public static Pattern parsePattern(MagpieParser parser) {
    if (parser.match(TokenType.BOOL)) {
      return new LiteralPattern(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      return new LiteralPattern(Expr.int_(parser.last(1).getInt()));
    } else if (parser.match(TokenType.STRING)) {
      return new LiteralPattern(Expr.string(parser.last(1).getString()));
    } else {
      Expr typeAnnotation = parser.parseTypeExpression();
      return new TypePattern(typeAnnotation);
    }
  }
  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.MATCH);
    
    List<Expr> exprs = new ArrayList<Expr>();
    
    // Parse the value.
    Expr value = parser.parseExpression();
    // TODO(bob): Need to make sure name is unique, and put it scope local to
    // match expr.
    exprs.add(Expr.var(value.getPosition(), "__value__", value));
    Expr valueExpr = Expr.name("__value__");
    
    // Require a newline between the value and the first case.
    parser.consume(TokenType.LINE);
        
    // Parse the cases.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    while (parser.match(TokenType.CASE)) {
      cases.add(parseCase(parser));
    }
    
    // Parse the else case, if present.
    Expr elseCase = null;
    if (parser.match("else")) {
      elseCase = parser.parseEndBlock();
    }
    
    parser.consume(TokenType.LINE);
    parser.consume("end");
    
    exprs.add(desugarCases(valueExpr, cases, elseCase));
    
    return Expr.block(exprs);
  }
}
