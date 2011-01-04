package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.ast.pattern.WildcardPattern;
import com.stuffwithstuff.magpie.interpreter.Name;
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
      List<Expr> bodyExprs = new ArrayList<Expr>();
      thisCase.getPattern().createBindings(bodyExprs, valueExpr);
      bodyExprs.add(body);
      body = Expr.block(bodyExprs);
      
      chained = new IfExpr(body.getPosition(), null, condition, body, chained);
    }

    return chained;
  }
  
  private static MatchCase parseCase(MagpieParser parser) {
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
    
    return new MatchCase(pattern, bodyParse.getKey());
  }
  
  public static Pattern parsePattern(MagpieParser parser) {
    // Valid patterns:
    // 1
    // a 1
    // b true
    // c
    // _
    // d Int
    // _ Int | String
    // f (Int, String)
    // g (Int => String) => Bool
    
    // See if there is a binding for the pattern.
    String name = null;
    if (parser.current().getType() == TokenType.NAME) {
      String token = parser.current().getString();
      if (token.equals("_") ||
          Character.isLowerCase(token.charAt(0))) {
        name = parser.consume().getString();
      }
    }

    // Parse the pattern itself.
    Pattern pattern = null;
    if (parser.match(TokenType.BOOL)) {
      pattern = new ValuePattern(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      pattern = new ValuePattern(Expr.int_(parser.last(1).getInt()));
    } else if (parser.match(TokenType.STRING)) {
      pattern = new ValuePattern(Expr.string(parser.last(1).getString()));
    } else if (parser.lookAheadAny(TokenType.NAME, TokenType.LEFT_PAREN)) {
      Expr expr = parser.parseTypeExpression();
      
      // The rule is, a name (or _) before a pattern indicates that it matches
      // against the value's type. Otherwise it does a straight equality test
      // against the value.
      if (name != null) {
        pattern = new TypePattern(expr);
      } else {
        pattern = new ValuePattern(expr);
      }
    }
    
    // A pattern may have a name, no name, or a wildcard name.
    // It may also have a further pattern expression, or not.
    // Each of those combinations has a different interpretation:
    //
    // no name,  no pattern -> Oops, error!
    // no name,  pattern    -> Just use the pattern.
    // name,     no pattern -> Straight variable pattern.
    // name,     pattern    -> Variable pattern with embedded pattern.
    // wildcard, no pattern -> Wildcard pattern.
    // wildcard, pattern    -> Use the pattern.
    //
    // The last case is a bit special since the wildcard is there but doesn't
    // affect the pattern. It's used to distinguish matching on the value's
    // *type* versus matching the value as *equal to a type*. For example:
    //
    // match foo
    //     case Int   then print("foo is the Int class object")
    //     case _ Int then print("foo's type is Int")
    // end
    
    if (name == null) {
      if (pattern == null) throw new ParseException(
          "Could not parse pattern.");
      
      return pattern;
    }
    
    if (name.equals("_")) {
      if (pattern == null) return new WildcardPattern();
      return pattern;
    }
    
    return new VariablePattern(name, pattern);
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
