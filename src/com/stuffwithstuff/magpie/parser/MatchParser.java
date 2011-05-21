package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class MatchParser implements PrefixParser {
  private static MatchCase parseCase(MagpieParser parser) {
    Pattern pattern = PatternParser.parse(parser);

    parser.consume("then");
    
    Pair<Expr, Token> bodyParse = parser.parseExpressionOrBlock("else", "end", "case");
    
    // Allow newlines to separate single-line case and else cases.
    if ((bodyParse.getValue() == null) &&
        (parser.lookAhead(TokenType.LINE, "case") ||
         parser.lookAhead(TokenType.LINE, "else"))) {
      parser.consume(TokenType.LINE);
    }
    
    return new MatchCase(pattern, bodyParse.getKey());
  }
  
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    
    // Parse the value.
    Expr value = parser.parseExpression();
    
    // Require a newline between the value and the first case.
    parser.consume(TokenType.LINE);
        
    // Parse the cases.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    while (parser.match("case")) {
      cases.add(parseCase(parser));
    }
    
    // Parse the else case, if present.
    if (parser.match("else")) {
      Expr elseCase = parser.parseExpressionOrBlock();
      cases.add(new MatchCase(elseCase));
    }
    
    parser.consume(TokenType.LINE);
    parser.consume("end");
    
    return Expr.match(span.end(), value, cases);
  }
}
