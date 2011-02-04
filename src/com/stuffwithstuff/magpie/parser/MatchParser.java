package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.util.Pair;

public class MatchParser extends PrefixParser {
  private static MatchCase parseCase(MagpieParser parser) {
    Pattern pattern = PatternParser.parse(parser);

    parser.consume(TokenType.THEN);
    
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
  public Expr parse(MagpieParser parser, Token token) {
    Position position = parser.last(1).getPosition();
    
    // Parse the value.
    Expr value = parser.parseExpression();
    
    // Require a newline between the value and the first case.
    parser.consume(TokenType.LINE);
        
    // Parse the cases.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    while (parser.match(TokenType.CASE)) {
      cases.add(parseCase(parser));
    }
    
    // Parse the else case, if present.
    if (parser.match("else")) {
      Expr elseCase = parser.parseEndBlock();
      cases.add(new MatchCase(new VariablePattern("_", null), elseCase));
    }
    
    parser.consume(TokenType.LINE);
    parser.consume("end");
    
    position = position.union(parser.last(1).getPosition());
    return Expr.match(position, value, cases);
  }
}
