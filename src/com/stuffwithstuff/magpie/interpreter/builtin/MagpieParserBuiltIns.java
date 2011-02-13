package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.JavaToMagpie;
import com.stuffwithstuff.magpie.interpreter.MagpieToJava;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.InfixParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.PatternParser;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.PrefixParser;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.Pair;

public class MagpieParserBuiltIns {
  @Shared
  @Signature("registerPrefixParser(keyword String, parser PrefixParser ->)")
  public static class RegisterPrefixParser implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.getTupleField(0).asString();
      Obj parser = arg.getTupleField(1);
      
      interpreter.registerParser(keyword,
          new MagpiePrefixParser(interpreter, parser));
      return interpreter.nothing();
    }
  }
  
  @Shared
  @Signature("registerInfixParser(keyword String, parser PrefixParser ->)")
  public static class RegisterInfixParser implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.getTupleField(0).asString();
      Obj parser = arg.getTupleField(1);
      
      interpreter.registerParser(keyword,
          new MagpieInfixParser(interpreter, parser));
      return interpreter.nothing();
    }
  }
  
  @Shared
  @Signature("reserveWord(keyword String ->)")
  public static class ReserveWord implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String keyword = arg.asString();
      
      interpreter.reserveWord(keyword);
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
        token = parser.consume(MagpieToJava.convertTokenType(interpreter, arg));
      }
      
      return JavaToMagpie.convert(interpreter, token);
    }
  }
  
  @Signature("consumeAny(tokenTypes -> Token)")
  public static class ConsumeAny implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      int numTypes = arg.getField("count").asInt();
      TokenType[] types = new TokenType[numTypes];
      for (int i = 0; i < numTypes; i++) {
        types[i] = MagpieToJava.convertTokenType(interpreter, arg.getTupleField(i));
      }

      Token token = parser.consumeAny(types);
      return JavaToMagpie.convert(interpreter, token);
    }
  }
  
  @Signature("lookAhead(tokens -> Bool)")
  public static class LookAhead implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      TokenType[] types = convertTokenTypes(interpreter, arg);
      return interpreter.createBool(parser.lookAhead(types));
    }
  }
  
  @Signature("lookAheadAny(tokens -> Bool)")
  public static class LookAheadAny implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      TokenType[] types = convertTokenTypes(interpreter, arg);
      return interpreter.createBool(parser.lookAheadAny(types));
    }
  }
  
  @Signature("matchKeyword(keyword -> Bool)")
  public static class MatchKeyword implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      String keyword = arg.asString();
      return interpreter.createBool(parser.match(keyword));
    }
  }
  
  @Signature("matchToken(token TokenType -> Bool)")
  public static class MatchToken implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      TokenType tokenType = MagpieToJava.convertTokenType(interpreter, arg);
      return interpreter.createBool(parser.match(tokenType));
    }
  }
  
  @Signature("parseBlock(keywords -> (Expression, Token | Nothing))")
  public static class ParseBlock implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      // TODO(bob): Hokey.
      Pair<Expr, Token> result;
      if (arg.getClassObj() == interpreter.getTupleClass()) {
        String keyword1 = arg.getTupleField(0).asString();
        String keyword2 = arg.getTupleField(1).asString();
        result = parser.parseBlock(keyword1, keyword2);
      } else if (arg.getClassObj() == interpreter.getGlobal("TokenType")) {
        TokenType token = MagpieToJava.convertTokenType(interpreter, arg);
        result = parser.parseBlock(token);
      } else {
        String keyword = arg.asString();
        result = parser.parseBlock(keyword);
      }
      
      Obj expr = JavaToMagpie.convert(interpreter, result.getKey());
      Obj token = JavaToMagpie.convert(interpreter, result.getValue());
      return interpreter.createTuple(expr, token);
    }
  }
  
  @Signature("parseEndBlock(-> Expression)")
  public static class ParseEndBlock implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseEndBlock();
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  @Signature("parseExpression(stickiness Int | Nothing -> Expression)")
  public static class ParseExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      int stickiness = 0;
      if (arg.getValue() instanceof Integer) {
        stickiness = arg.asInt();
      }
      
      Expr expr = parser.parseExpression(stickiness);
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  @Signature("parseFunction(-> Expression)")
  public static class ParseFunction implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseFunction();
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  @Signature("parsePattern(-> Pattern)")
  public static class ParsePattern implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Pattern pattern = PatternParser.parse(parser);
      
      return JavaToMagpie.convert(interpreter, pattern);
    }
  }
  
  @Signature("parseTypeExpression(-> Expression)")
  public static class ParseTypeExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      MagpieParser parser = (MagpieParser) thisObj.getValue();
      
      Expr expr = parser.parseTypeAnnotation();
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  private static TokenType[] convertTokenTypes(Interpreter interpreter, Obj arg) {
    TokenType[] types;
    if (arg.getClassObj() == interpreter.getTupleClass()) {
      int numTypes = arg.getField("count").asInt();
      types = new TokenType[numTypes];
      for (int i = 0; i < numTypes; i++) {
        types[i] = MagpieToJava.convertTokenType(interpreter, arg.getTupleField(i));
      }
    } else {
      // Just one token type.
      types = new TokenType[1];
      types[0] = MagpieToJava.convertTokenType(interpreter, arg);
    }
    
    return types;
  }
  
  private static class MagpiePrefixParser extends PrefixParser {
    public MagpiePrefixParser(Interpreter interpreter, Obj parser) {
      mInterpreter = interpreter;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Token token) {
      // Wrap the Java parser in a Magpie one.
      Obj parserObj = mInterpreter.instantiate(
          mInterpreter.getMagpieParserClass(), parser);
      Obj tokenObj = JavaToMagpie.convert(mInterpreter, token);
      Obj arg = mInterpreter.createTuple(parserObj, tokenObj);
      
      // Let the Magpie code do the parsing.
      Obj expr = mInterpreter.invokeMethod(mParser, "parse", arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mInterpreter, expr);
    }
    
    private Interpreter mInterpreter;
    private Obj mParser;
  }
  
  private static class MagpieInfixParser extends InfixParser {
    public MagpieInfixParser(Interpreter interpreter, Obj parser) {
      mInterpreter = interpreter;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Expr left, Token token) {
      // Wrap the Java parser in a Magpie one.
      Obj parserObj = mInterpreter.instantiate(
          mInterpreter.getMagpieParserClass(), parser);
      Obj exprObj = JavaToMagpie.convert(mInterpreter, left);
      Obj tokenObj = JavaToMagpie.convert(mInterpreter, token);
      Obj arg = mInterpreter.createTuple(parserObj, exprObj, tokenObj);
      
      // Let the Magpie code do the parsing.
      Obj expr = mInterpreter.invokeMethod(mParser, "parse", arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mInterpreter, expr);
    }
    
    @Override
    public int getStickiness() {
      // Ask the Magpie object.
      Obj stickiness = mInterpreter.getMember(Position.none(), mParser, "stickiness");
      return stickiness.asInt();
    }
    
    private Interpreter mInterpreter;
    private Obj mParser;
  }
}
