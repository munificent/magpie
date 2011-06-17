package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;

public class DocBuilder {

  public DocBuilder() {
    builder = new StringBuilder();
  }
  
  public DocBuilder append(String name, Multimethod multimethod) {
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

    return this;
  }
  
  public DocBuilder append(String string) {
    builder.append(string);
    return this;
  }

  /**
   * Override this to filter out the set of method specializations that get shown.
   */
  protected boolean shouldDisplayMethod(Callable callable) {
    return true;
  }

  private StringBuilder builder;
}
