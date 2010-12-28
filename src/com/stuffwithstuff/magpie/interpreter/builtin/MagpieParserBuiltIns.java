package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ExprConverter;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.ExprParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.TokenType;

public class MagpieParserBuiltIns {
  @Shared
  @Signature("registerKeyword(keyword String, parser KeywordParser ->)")
  public static class RegisterKeyword implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.getTupleField(0).asString();
      Obj parser = arg.getTupleField(1);
      
      interpreter.registerKeywordParser(keyword,
          new MagpieExprParser(interpreter, parser));
      return interpreter.nothing();
    }
  }
  
  @Signature("consume(token TokenType | String ->)")
  public static class Consume implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      if (arg.getClassObj() == interpreter.getStringClass()) {
        // Parsing a name token with the given name.
        // Note: we're doing this explicitly instead of just calling
        //   parser.consume(TokenType.NAME);
        // because that explicitly does *not* consume keyword tokens, which are
        // exactly the tokens we *do* want to consume here.
        if (parser.current().getType() != TokenType.NAME) {
          // TODO(bob): Better error reporting.
          return interpreter.throwError("ParseError");
        }
        if (!parser.current().getString().equals(arg.asString())) {
          // TODO(bob): Better error reporting.
          return interpreter.throwError("ParseError");
        }
        parser.consume();
      } else {
        parser.consume(convertType(interpreter, arg));
      }
      return interpreter.nothing();
    }
  }

  @Signature("lookAheadAny(tokens -> Bool)")
  public static class LookAheadAny implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      // TODO(bob): Hackish. Assumes arg is tuple.
      int count = arg.getField("count").asInt();
      TokenType[] types = new TokenType[count];
      for (int i = 0; i < count; i++) {
        types[i] = convertType(interpreter, arg.getTupleField(i));
      }
      
      return interpreter.createBool(parser.lookAheadAny(types));
    }
  }
  
  private static TokenType convertType(Interpreter interpreter, Obj tokenType) {
    // Note: the values here must be kept in sync with the order that they
    // are defined in Token.mag.
    TokenType type;
    switch (tokenType.getField("value").asInt()) {
    case 0: type = TokenType.LEFT_PAREN; break;
    case 1: type = TokenType.RIGHT_PAREN; break;
    case 2: type = TokenType.LEFT_BRACKET; break;
    case 3: type = TokenType.RIGHT_BRACKET; break;
    case 4: type = TokenType.LEFT_BRACE; break;
    case 5: type = TokenType.RIGHT_BRACE; break;
    case 6: type = TokenType.COMMA; break;
    case 7: type = TokenType.DOT; break;
    case 8: type = TokenType.EQUALS; break;
    case 9: type = TokenType.LINE; break;
    case 10: type = TokenType.NAME; break;
    case 11: type = TokenType.FIELD; break;
    case 12: type = TokenType.OPERATOR; break;
    case 13: type = TokenType.BOOL; break;
    case 14: type = TokenType.INT; break;
    case 15: type = TokenType.STRING; break;
    default:
      // TODO(bob): Better error reporting.
      interpreter.throwError("ParseError");
      type = TokenType.EOF;
    }
    
    return type;
  }
  
  @Signature("parseExpression(-> Expression)")
  public static class ParseExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      return ExprConverter.convert(interpreter, parser.parseExpression(), 
          interpreter.createTopLevelContext());
    }
  }

  private static class MagpieExprParser implements ExprParser {
    public MagpieExprParser(Interpreter interpreter, Obj parser) {
      mInterpreter = interpreter;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser) {
      // Parser is assumed to implement:
      // interface KeywordParser
      //     def parse(parser MagpieParser -> Expression)
      // end

      // Wrap the Java parser in a Magpie one.
      Obj parserObj = mInterpreter.instantiate(
          mInterpreter.getMagpieParserClass(), parser);
      
      // Let the Magpie code do the parsing.
      Obj expr = mInterpreter.invokeMethod(mParser, "parse", parserObj);
      
      // Marshall it back to Java format.
      return ExprConverter.convert(mInterpreter, expr);
    }
    
    private Interpreter mInterpreter;
    private Obj mParser;
  }
}
