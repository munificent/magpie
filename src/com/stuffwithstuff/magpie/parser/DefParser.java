package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;

/**
 * Parses a method definition.
 */
public class DefParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    // Receiver-less function:  def abs(value Int) Int
    // Method on any receiver:  def this is(type Type) Bool
    // No-arg method:           def String reverse() String
    // Getter:                  def Point magnitude Int
    // Setter:                  def Rect width Int =
    // Assignment method:       def Array call(index Int) = Int
    // Callable:                def String call(index Int) Char
    
    // TODO(bob): Support other method flavors...
    Pattern leftHandPattern;
    String name;
    
    if (parser.match("shared")) {
      // def shared Foo blah() ...
      // Defines a shared method on Foo by specializing it on the value of the
      // class object.
      // TODO(bob): Mostly temp syntax...
      Expr classObj = Expr.name(parser.consume(TokenType.NAME).getString());
      leftHandPattern = new ValuePattern(classObj);
      name = parser.consume(TokenType.NAME).getString();
    } else if (parser.lookAhead(TokenType.NAME, TokenType.LEFT_PAREN)) {
      // No receiver.
      leftHandPattern = new VariablePattern("_", null);
      name = parser.consume().getString();
    } else if (parser.match("this")) {
      // Any receiver.
      leftHandPattern = new VariablePattern("this_", null);
      name = parser.consume(TokenType.NAME).getString();
    } else if (parser.match(TokenType.NAME)) {
      // Simple type receiver.
      leftHandPattern = new VariablePattern("this_",
          Expr.name(parser.last(1).getString()));
      name = parser.consume(TokenType.NAME).getString();
    } else {
      throw new ParseException("Don't know how to parse method.");
    }

    // Parse the right-hand type signature.
    parser.consume(TokenType.LEFT_PAREN);
    Pattern rightHandPattern;
    if (!parser.lookAhead(TokenType.RIGHT_PAREN)) {
      rightHandPattern = PatternParser.parse(parser);
    } else {
      // () means no arguments allowed.
      rightHandPattern = new ValuePattern(Expr.nothing());
    }
    parser.consume(TokenType.RIGHT_PAREN);
    
    // Parse the body.
    Expr body = parser.parseEndBlock();
    
    // Combine the left and right patterns.
    List<Pattern> fields = new ArrayList<Pattern>();
    fields.add(leftHandPattern);
    fields.add(rightHandPattern);
    Pattern pattern = new TuplePattern(fields);
    
    FnExpr function = Expr.fn(token.getPosition(), pattern, body);
    
    return Expr.message(token.getPosition(), null, "defineMethod",
        Expr.tuple(Expr.string(name), function));
  }
}
