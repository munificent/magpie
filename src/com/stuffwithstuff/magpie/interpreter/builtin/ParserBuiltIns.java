package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.JavaToMagpie;
import com.stuffwithstuff.magpie.interpreter.MagpieToJava;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.InfixParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.PrefixParser;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;

public class ParserBuiltIns {
  @Signature("definePrefix(keyword is String, parser)")
  public static class DefinePrefix implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      String keyword = arg.getField(1).getField(0).asString();
      Obj parser = arg.getField(1).getField(1);
      
      context.getModule().defineSyntax(keyword,
          new MagpiePrefixParser(context, parser));
      
      return context.nothing();
    }
  }
  
  @Signature("defineInfix(keyword is String, stickiness is Int, parser)")
  public static class DefineInfix implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      String keyword = arg.getField(1).getField(0).asString();
      int stickiness = arg.getField(1).getField(1).asInt();
      Obj parser = arg.getField(1).getField(2);

      // TODO(bob): Hack! Wrong. Needs to define it in the module that's
      // calling this. Once built-ins have access to the calling module, fix
      // this.
      context.getModule().defineSyntax(keyword,
          new MagpieInfixParser(context, stickiness, parser));
      
      return context.nothing();
    }
  }
  
  @Signature("(this is Parser) matchToken(token is TokenType)")
  public static class MatchToken implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      MagpieParser parser = (MagpieParser) arg.getField(0).getValue();
      
      TokenType tokenType = MagpieToJava.convertTokenType(context.getInterpreter(),
          arg.getField(1));
      return context.toObj(parser.match(tokenType));
    }
  }
  
  @Signature("(this is Parser) parseExpression(stickiness is Int)")
  public static class ParseExpression implements BuiltInCallable {
    @Override
    public Obj invoke(Context context, Obj arg) {
      MagpieParser parser = (MagpieParser) arg.getField(0).getValue();

      Expr expr = parser.parseExpression(arg.getField(1).asInt());
      
      return JavaToMagpie.convert(context, expr);
    }
  }
  
  private static class MagpiePrefixParser implements PrefixParser {
    public MagpiePrefixParser(Context context, Obj parser) {
      mContext = context;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mContext.getInterpreter().getSyntaxModule().getExportedVariables().get("Parser").asClass();
      Obj parserObj = mContext.getInterpreter().instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mContext, token);
      Obj arg = mContext.toObj(parserObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call the parser.
      Obj expr = mContext.getInterpreter().invoke(mParser, "call", arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mContext.getInterpreter(), expr);
    }
    
    private final Context mContext;
    private final Obj mParser;
  }
  
  private static class MagpieInfixParser implements InfixParser {
    public MagpieInfixParser(Context context, int stickiness, Obj parser) {
      mContext = context;
      mStickiness = stickiness;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Expr left, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mContext.getInterpreter().getSyntaxModule().getExportedVariables().get("Parser").asClass();
      Obj parserObj = mContext.getInterpreter().instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mContext, token);
      Obj leftObj = JavaToMagpie.convert(mContext, left);
      Obj arg = mContext.toObj(parserObj, leftObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call the parser.
      Obj expr = mContext.getInterpreter().invoke(mParser, "call", arg);
       
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mContext.getInterpreter(), expr);
    }
    
    @Override
    public int getStickiness() {
      return mStickiness;
    }
    
    private final Context mContext;
    private final int mStickiness;
    private final Obj mParser;
  }
}
