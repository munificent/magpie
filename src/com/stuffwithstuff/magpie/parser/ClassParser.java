package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class ClassParser implements PrefixParser {  
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    String name = parser.consume(TokenType.NAME).getString();
    
    // Parse the parents, if any.
    List<String> parents = new ArrayList<String>();
    if (parser.match(TokenType.COLON)) {
      do {
        parents.add(parser.consume(TokenType.NAME).getString());
      } while (parser.match(TokenType.COMMA));
    }
    
    parser.consume(TokenType.LINE);

    // Parse the doc comment if given.
    String doc = "";
    if (parser.match(TokenType.DOC_COMMENT, TokenType.LINE)) {
      doc = parser.last(2).getString();
    }
    
    Map<String, Field> fields = new HashMap<String, Field>();
    
    // Parse the body.
    while (!parser.match("end")) {
      if (parser.match("var")) parseField(parser, true, fields);
      else if (parser.match("val")) parseField(parser, false, fields);

      parser.consume(TokenType.LINE);
    }
    
    return Expr.class_(span.end(), doc, name, parents, fields);
  }

  private void parseField(MagpieParser parser, boolean isMutable,
      Map<String, Field> fields) {
    String name = parser.consume(TokenType.NAME).getString();
    
    // Parse the pattern if there is one.
    Pattern pattern;
    if (parser.lookAhead(TokenType.EQUALS) ||
        parser.lookAhead(TokenType.LINE)) {
      pattern = Pattern.wildcard();
    } else {
      pattern = PatternParser.parse(parser);
    }
    
    // Parse the initializer if there is one.
    Expr initializer;
    if (parser.match(TokenType.EQUALS)) {
      initializer = parser.parseExpressionOrBlock();
    } else {
      initializer = null;
    }
    
    fields.put(name, new Field(isMutable, initializer, pattern));
  }
}
