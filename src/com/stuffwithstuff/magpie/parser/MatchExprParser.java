package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

// TODO(bob): Instead of desugaring patterns, they should be first-class
// constructs in the interpreter and type-checker. (In fact, if expressions
// should desugar to *them*).
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
    // By default if no case matches, an error will be thrown.
    if (elseExpr == null) {
      elseExpr = Expr.message(Expr.name("Runtime"), "throw",
          Expr.message(Expr.name("NoMatchError"), Name.NEW, Expr.nothing()));
    }
    
    // Desugar the cases to a series of chained if/thens.
    Expr chained = elseExpr;
    for (int i = cases.size() - 1; i >= 0; i--) {
      MatchCase thisCase = cases.get(i);

      Expr condition = thisCase.getPattern().createPredicate(valueExpr);
      Expr body = thisCase.getBody();
      
      // Bind the names.
      Expr binding = Expr.var(body.getPosition(), thisCase.getPattern(),
          valueExpr);
      body = Expr.block(binding, body);
      
      chained = new IfExpr(body.getPosition(), null, condition, body, chained);
    }

    return chained;
  }
  
  private static MatchCase parseCase(MagpieParser parser) {
    Pattern pattern = PatternParser.parse(parser);

    parser.consume("then");
    
    Pair<Expr, Token> bodyParse = parser.parseBlock("else", "end",
        TokenType.CASE);
    
    // Allow newlines to separate single-line case and else cases.
    if ((bodyParse.getValue() == null) &&
        (parser.lookAhead(TokenType.LINE, TokenType.CASE) ||
         parser.lookAhead(TokenType.LINE, "else"))) {
      parser.consume(TokenType.LINE);
    }
    
    return new MatchCase(pattern, bodyParse.getKey());
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
