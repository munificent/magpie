package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.JavaToMagpie;
import com.stuffwithstuff.magpie.interpreter.MagpieToJava;
import com.stuffwithstuff.magpie.interpreter.Multimethod;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.PrefixParser;
import com.stuffwithstuff.magpie.parser.StringCharacterReader;
import com.stuffwithstuff.magpie.parser.Token;

public class ParserBuiltIns {
  @Signature("defineSyntax(keyword is String, parser is PrefixParser)")
  public static class DefineParser implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String keyword = arg.getField(1).getField(0).asString();
      Obj parser = arg.getField(1).getField(1);
      
      interpreter.getGrammar().defineParser(keyword,
          new MagpiePrefixParser(interpreter, parser));
      return interpreter.nothing();
    }
  }
  
  @Signature("(this is Parser) parseExpression()")
  public static class ParseExpression implements BuiltInCallable {
    @Override
    public Obj invoke(Interpreter interpreter, Obj arg) {
      MagpieParser parser = (MagpieParser) arg.getField(0).getValue();

      Expr expr = parser.parseExpression();
      
      return JavaToMagpie.convert(interpreter, expr);
    }
  }
  
  private static class MagpiePrefixParser implements PrefixParser {
    public MagpiePrefixParser(Interpreter interpreter, Obj parser) {
      mInterpreter = interpreter;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mInterpreter.getSyntaxModule().getExportedVariables().get("Parser").asClass();
      Obj parserObj = mInterpreter.instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mInterpreter, token);
      Obj arg = mInterpreter.createRecord(parserObj, tokenObj);
      
      // Let the Magpie code do the parsing.
      Multimethod parseMethod = mInterpreter.getSyntaxModule().getExportedMultimethods().get("parse");
      Obj expr = parseMethod.invoke(mInterpreter, mParser, arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mInterpreter, expr);
    }
    
    private Interpreter mInterpreter;
    private Obj mParser;
  }
}
