package com.stuffwithstuff.magpie.intrinsic;

import java.util.List;

import com.stuffwithstuff.magpie.Def;
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
import com.stuffwithstuff.magpie.util.Pair;

public class SyntaxMethods {
  @Def("definePrefix(keyword is String, parser)")
  public static class DefinePrefix implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String keyword = right.getField(0).asString();
      Obj parser = right.getField(1);
      
      context.getModule().defineSyntax(keyword,
          new MagpiePrefixParser(context, parser), true);
      
      return context.nothing();
    }
  }
  
  @Def("defineInfix(keyword is String, precedence is Int, parser)")
  public static class DefineInfix implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String keyword = right.getField(0).asString();
      int precedence = right.getField(1).asInt();
      Obj parser = right.getField(2);

      context.getModule().defineSyntax(keyword,
          new MagpieInfixParser(context, precedence, parser), true);
      
      return context.nothing();
    }
  }

  @Def("(this is Parser) consume(keyword is String)")
  public static class Consume_Keyword implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();
      String keyword = right.asString();

      return JavaToMagpie.convert(context, parser.consume(keyword));
    }
  }

  @Def("(this is Parser) matchToken(token is String)")
  public static class MatchToken_String implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();
      String keyword = right.asString();
      
      return context.toObj(parser.match(keyword));
    }
  }

  @Def("(this is Parser) matchToken(token is TokenType)")
  public static class MatchToken_TokenType implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();
      
      TokenType tokenType = MagpieToJava.convertTokenType(
          context, right);
      return context.toObj(parser.match(tokenType));
    }
  }
  
  @Def("(this is Parser) parseExpression(precedence is Int)")
  public static class ParseExpression implements Intrinsic {
    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();

      Expr expr = parser.parseExpression(right.asInt());
      
      return JavaToMagpie.convert(context, expr);
    }
  }
  
  @Def("(this is Parser) parseExpressionOrBlock(keywords is Array)")
  public static class ParseExpressionOrBlock_List implements Intrinsic {
    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();

      List<Obj> keywordObjs = right.asList();
      String[] keywords = new String[keywordObjs.size()];
      for (int i = 0; i < keywords.length; i++) {
        keywords[i] = keywordObjs.get(i).asString();
      }
      
      Pair<Expr, Token> result = parser.parseExpressionOrBlock(keywords);
      
      Obj expr = JavaToMagpie.convert(context, result.getKey());
      Obj token = JavaToMagpie.convert(context, result.getValue());
      return context.toObj(expr, token);
    }
  }
  
  @Def("(this is Parser) parseExpressionOrBlock()")
  public static class ParseExpressionOrBlock_Nothing implements Intrinsic {
    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      MagpieParser parser = (MagpieParser) left.getValue();

      Expr expr = parser.parseExpressionOrBlock();
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
      ClassObj parserClass = mContext.getInterpreter().getSyntaxModule().getExportedVariable("Parser").asClass();
      Obj parserObj = mContext.getInterpreter().instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mContext, token);
      Obj arg = mContext.toObj(parserObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call the parser.
      Obj expr = mContext.getInterpreter().invoke(mParser, "call", arg);
      
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mContext, expr);
    }
    
    private final Context mContext;
    private final Obj mParser;
  }
  
  private static class MagpieInfixParser implements InfixParser {
    public MagpieInfixParser(Context context, int precedence, Obj parser) {
      mContext = context;
      mPrecedence = precedence;
      mParser = parser;
    }
    
    @Override
    public Expr parse(MagpieParser parser, Expr left, Token token) {
      // Wrap the Java parser in a Magpie one.
      ClassObj parserClass = mContext.getInterpreter().getSyntaxModule().getExportedVariable("Parser").asClass();
      Obj parserObj = mContext.instantiate(parserClass, parser);
      
      Obj tokenObj = JavaToMagpie.convert(mContext, token);
      Obj leftObj = JavaToMagpie.convert(mContext, left);
      Obj arg = mContext.toObj(parserObj, leftObj, tokenObj);
      
      // Let the Magpie code do the parsing. Call the parser.
      Obj expr = mContext.getInterpreter().invoke(mParser, "call", arg);
       
      // Marshall it back to Java format.
      return MagpieToJava.convertExpr(mContext, expr);
    }
    
    @Override
    public int getPrecedence() {
      return mPrecedence;
    }
    
    private final Context mContext;
    private final int mPrecedence;
    private final Obj mParser;
  }
}
