package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.ExprConverter;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.ExprParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.Expect;

public class MagpieParserBuiltIns {
  @Shared
  @Signature("registerParseword(keyword String, parser KeywordParser ->)")
  public static class RegisterParseword implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.getTupleField(0).asString();
      Obj parser = arg.getTupleField(1);
      
      interpreter.registerParseword(keyword,
          new MagpieExprParser(interpreter, parser));
      return interpreter.nothing();
    }
  }
  
  @Shared
  @Signature("registerKeyword(keyword String ->)")
  public static class RegisterKeyword implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.asString();
      
      interpreter.registerKeyword(keyword);
      return interpreter.nothing();
    }
  }
  
  @Signature("consume(token TokenType | String | Nothing -> Token)")
  public static class Consume implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Token token;
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
        token = parser.consume();
      } else if (arg == interpreter.nothing()) {
        token = parser.consume();
      } else {
        token = parser.consume(convertType(interpreter, arg));
      }
      
      return convertToken(interpreter, token);
    }
  }
  
  @Signature("consumeAny(tokenTypes -> Token)")
  public static class ConsumeAny implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      int numTypes = arg.getField("count").asInt();
      TokenType[] types = new TokenType[numTypes];
      for (int i = 0; i < numTypes; i++) {
        types[i] = convertType(interpreter, arg.getTupleField(i));
      }

      Token token = parser.consumeAny(types);
      return convertToken(interpreter, token);
    }
  }
  
  @Signature("lookAhead(tokens -> Bool)")
  public static class LookAhead implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      TokenType[] types;
      if (arg.getClassObj() == interpreter.getTupleClass()) {
        int numTypes = arg.getField("count").asInt();
        types = new TokenType[numTypes];
        for (int i = 0; i < numTypes; i++) {
          types[i] = convertType(interpreter, arg.getTupleField(i));
        }
      } else {
        // Just one token type.
        types = new TokenType[1];
        types[0] = convertType(interpreter, arg);
      }
      return interpreter.createBool(parser.lookAhead(types));
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
  
  @Signature("parseBlock(-> Expression)")
  public static class ParseBlock implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseEndBlock();
      
      return ExprConverter.convert(interpreter, expr, 
          interpreter.createTopLevelContext());
    }
  }
  
  @Signature("parseExpression(-> Expression)")
  public static class ParseExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseExpression();
      
      return ExprConverter.convert(interpreter, expr, 
          interpreter.createTopLevelContext());
    }
  }
  
  @Signature("parseFunction(-> Expression)")
  public static class ParseFunction implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseFunction();
      
      return ExprConverter.convert(interpreter, expr, 
          interpreter.createTopLevelContext());
    }
  }
  
  @Signature("parseTypeExpression(-> Expression)")
  public static class ParseTypeExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseTypeExpression();
      
      return ExprConverter.convert(interpreter, expr, 
          interpreter.createTopLevelContext());
    }
  }
  
  private static TokenType convertType(Interpreter interpreter, Obj tokenType) {
    Obj value = tokenType.getField("value");
    Expect.notNull(value);
    
    // Note: the values here must be kept in sync with the order that they
    // are defined in Token.mag.
    TokenType type;
    switch (value.asInt()) {
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
  
  private static Obj convertToken(Interpreter interpreter, Token token) {
    // Note: the values here must be kept in sync with the order that they
    // are defined in Token.mag.
    int tokenTypeValue;
    String tokenTypeName;
    switch (token.getType()) {
    case LEFT_PAREN: tokenTypeValue = 0; tokenTypeName = "leftParen"; break;
    case RIGHT_PAREN: tokenTypeValue = 1; tokenTypeName = "rightParen"; break;
    case LEFT_BRACKET: tokenTypeValue = 2; tokenTypeName = "leftBracket"; break;
    case RIGHT_BRACKET: tokenTypeValue = 3; tokenTypeName = "rightBracket"; break;
    case LEFT_BRACE: tokenTypeValue = 4; tokenTypeName = "leftBrace"; break;
    case RIGHT_BRACE: tokenTypeValue = 5; tokenTypeName = "leftBrace"; break;
    case COMMA: tokenTypeValue = 6; tokenTypeName = "comma"; break;
    case DOT: tokenTypeValue = 7; tokenTypeName = "dot"; break;
    case EQUALS: tokenTypeValue = 8; tokenTypeName = "equals"; break;
    case LINE: tokenTypeValue = 9; tokenTypeName = "line"; break;

    case NAME: tokenTypeValue = 10; tokenTypeName = "identifier"; break;
    case FIELD: tokenTypeValue = 11; tokenTypeName = "field"; break;
    case OPERATOR: tokenTypeValue = 12; tokenTypeName = "operator"; break;

    case BOOL: tokenTypeValue = 13; tokenTypeName = "boolLiteral"; break;
    case INT: tokenTypeValue = 14; tokenTypeName = "intLiteral"; break;
    case STRING: tokenTypeValue = 15; tokenTypeName = "stringLiteral"; break;

    default:
      // TODO(bob): Better error reporting.
      interpreter.throwError("ParseError");
      tokenTypeValue = -1;    
      tokenTypeName = "";
    }
    
    ClassObj tokenTypeClass = (ClassObj) interpreter.getGlobal("TokenType");
    Obj tokenType = interpreter.instantiate(tokenTypeClass, null);
    tokenType.setField("value", interpreter.createInt(tokenTypeValue));
    tokenType.setField("name", interpreter.createString(tokenTypeName));
    
    ClassObj tokenClass = (ClassObj) interpreter.getGlobal("Token");
    Obj tokenObj = interpreter.instantiate(tokenClass, null);
    tokenObj.setField("tokenType", tokenType);
    
    if (token.getValue() instanceof Boolean) {
      tokenObj.setField("value", interpreter.createBool(token.getBool()));
    } else if (token.getValue() instanceof Integer) {
      tokenObj.setField("value", interpreter.createInt(token.getInt()));
    } else if (token.getValue() instanceof String) {
      tokenObj.setField("value", interpreter.createString(token.getString()));
    } else {
      tokenObj.setField("value", interpreter.nothing());
    }
    
    return tokenObj;
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
