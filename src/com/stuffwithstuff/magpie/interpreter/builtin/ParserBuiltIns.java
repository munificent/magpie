package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.JavaToMagpie;
import com.stuffwithstuff.magpie.interpreter.MagpieToJava;
import com.stuffwithstuff.magpie.interpreter.Multimethod;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.InfixParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.PrefixParser;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;

public class ParserBuiltIns {
  @Signature("definePrefix(keyword is String)")
  public static class DefinePrefix implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String keyword = arg.getField(1).getField(0).asString();
      
      interpreter.getGrammar().defineParser(keyword,
          new MagpiePrefixParser(interpreter));
      return interpreter.nothing();
    }
  }
  
  @Signature("defineInfix(keyword is String, stickiness is Int)")
  public static class DefineInfix implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String keyword = arg.getField(1).getField(0).asString();
      int stickiness = arg.getField(1).getField(1).asInt();
      
      interpreter.getGrammar().defineParser(keyword,
          new MagpieInfixParser(interpreter, stickiness));
      return interpreter.nothing();
    }
  }
  
  @Signature("(this is Parser) matchToken(token is TokenType)")
  public static class MatchToken implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      MagpieParser parser = (MagpieParser) arg.getField(0).getValue();
      
      TokenType tokenType = MagpieToJava.convertTokenType(interpreter,
          arg.getField(1));
      return interpreter.createBool(parser.match(tokenType));
    }
  }
  
  @Signature("(this is Parser) parseExpression(stickiness is Int)")
  public static class ParseExpression implements BuiltInCallable {
    @Override
    public Obj invoke(Interpreter interpreter, Obj arg) {
      MagpieParser parser = (MagpieParser) arg.getField(0).getValue();

      Expr expr = parser.parseExpression(arg.getField(1).asInt());
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  private static class MagpiePrefixParser implements PrefixParser {
    public MagpiePrefixParser(Interpreter interpreter) {
      mInterpreter = interpreter;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mInterpreter.getSyntaxModule().getExportedVariables().get("Parser").asClass();
      Obj parserObj = mInterpreter.instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mInterpreter, token);
      Obj arg = mInterpreter.createRecord(parserObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call parse with the keyword as
      // the receiver. A parse method should then specialize to that keyword.
      Multimethod parseMethod = mInterpreter.getSyntaxModule().getExportedMultimethods().get("parse");
      Obj expr = parseMethod.invoke(mInterpreter,
          mInterpreter.createString(token.getString()), arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mInterpreter, expr);
    }
    
    private Interpreter mInterpreter;
  }
  
  private static class MagpieInfixParser implements InfixParser {
    public MagpieInfixParser(Interpreter interpreter, int stickiness) {
      mInterpreter = interpreter;
      mStickiness = stickiness;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Expr left, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mInterpreter.getSyntaxModule().getExportedVariables().get("Parser").asClass();
      Obj parserObj = mInterpreter.instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mInterpreter, token);
      Obj leftObj = JavaToMagpie.convert(mInterpreter, left);
      Obj arg = mInterpreter.createRecord(parserObj, leftObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call parse with the keyword as
      // the receiver. A parse method should then specialize to that keyword.
      Multimethod parseMethod = mInterpreter.getSyntaxModule().getExportedMultimethods().get("parse");
      Obj expr = parseMethod.invoke(mInterpreter,
          mInterpreter.createString(token.getString()), arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mInterpreter, expr);
    }
    
    @Override
    public int getStickiness() {
      return mStickiness;
    }
    
    private final Interpreter mInterpreter;
    private final int mStickiness;
  }
}
