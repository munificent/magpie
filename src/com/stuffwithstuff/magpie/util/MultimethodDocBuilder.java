package com.stuffwithstuff.magpie.util;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Multimethod;
import com.stuffwithstuff.magpie.interpreter.Name;

public class MultimethodDocBuilder {

  public String buildDoc(String name, Multimethod multimethod) {
    return appendDoc(name, multimethod, new StringBuilder()).toString();
  }

  public StringBuilder appendDoc(String name, Multimethod multimethod, StringBuilder builder) {
    if (multimethod.getDoc().length() > 0) {
      builder.append(name).append("\n");
      builder.append(multimethod.getDoc()).append("\n");
      builder.append("\n");
    }

    for (Callable method : multimethod.getMethods()) {
      if(shouldDisplayMethod(method)) {
        RecordPattern pattern = (RecordPattern) method.getPattern();
        Pattern leftParam = pattern.getFields().get(Name.getTupleField(0));
        Pattern rightParam = pattern.getFields().get(Name.getTupleField(1));
  
        String leftText = leftParam.toString();
        if (leftText.equals("nothing")) {
          leftText = "";
        } else {
          leftText = "(" + leftText + ") ";
        }
  
        String rightText = rightParam.toString();
        if (rightText.equals("nothing")) {
          if (leftText.equals("")) {
            rightText = "()";
          } else {
            rightText = "";
          }
        } else {
          rightText = "(" + rightText + ")";
        }
  
        builder.append(leftText).append(name).append(rightText).append("\n");
        if (method.getDoc().length() > 0) {
          builder.append("  " + method.getDoc().replace("\n", "\n  ") + "\n");
        } else {
          builder.append("  No documentation.\n");
        }
      }
    }

    return builder;
  }

  protected boolean shouldDisplayMethod(Callable callable) {
    return true;
  }

}
