package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.stuffwithstuff.magpie.Term.ForeColor;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.CharacterReader;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;

/**
 * Provides a string of characters by reading them from the user a line at a
 * time, as requested.
 */
public class NiceReplCharacterReader implements CharacterReader {
  public NiceReplCharacterReader(Interpreter interpreter) {
    mInterpreter = interpreter;
    
    InputStreamReader converter = new InputStreamReader(System.in);
    mInput = new BufferedReader(converter);
  }
  
  @Override
  public char current() {
    while (mPosition >= mLine.length()) {
      readLine();
    }

    return mLine.charAt(mPosition);
  }
  
  @Override
  public void advance() {
    if (mPosition < mLine.length()) {
      mPosition++;
    } else {
      readLine();
    }
  }

  @Override
  public String lookAhead(int count) {
    if (mPosition >= mLine.length()) return "";
    
    int endIndex = Math.min(mPosition + count, mLine.length());
    return mLine.substring(mPosition, endIndex);
  }

  private void readLine() {
    String prompt = ">> ";
    if (mFirstLine) {
      mFirstLine = false;
    } else {
      prompt = " | ";
    }
    
    Term.set(Term.ForeColor.GRAY);
    System.out.print(prompt);
    Term.set(Term.ForeColor.WHITE);
    
    try {
      mLine = mInput.readLine();
      
      // Rewrite it.
      colorLine(prompt, mLine);
      
      mLine += "\n";
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    mPosition = 0;
  }
  
  private void colorLine(String prompt, String line) {
    Lexer lexer = new Lexer("", new StringCharacterReader(line));

    Term.moveUp();

    // Redraw the prompt.
    Term.set(Term.ForeColor.GRAY);
    System.out.print(prompt);
    
    int length = 1;
    while (true) {
      Token token = lexer.readToken();
      if (token.getType() == TokenType.EOF) break;
      
      switch (token.getType()) {
        case BACKTICK:
        case COMMA:
        case DOT:
        case EQUALS:
          Term.set(Term.ForeColor.GRAY);
          break;
        
        // identifiers
        case NAME:
          if (mInterpreter.isReservedWord(token.getString())) {
            Term.set(ForeColor.CYAN);
          } else {
            Term.set(ForeColor.WHITE);
          }
          break;
          
        case FIELD:
          Term.set(Term.ForeColor.GRAY);
          break;
          
        case OPERATOR:
          Term.set(Term.ForeColor.WHITE);
          break;
  
        case TYPE_PARAM:
          Term.set(Term.ForeColor.GREEN);
          break;
  
        // literals
        case BOOL:
        case INT:
          Term.set(Term.ForeColor.LIGHT_BLUE);
          break;
          
        case DOUBLE:
        case STRING:
          Term.set(Term.ForeColor.YELLOW);
          break;
        
        // special identifiers
        case NOTHING:
        case THIS:
          Term.set(Term.ForeColor.LIGHT_BLUE);
          break;
          
        // keywords
        case ARROW:
        case CASE:
        case CATCH:
        case FN:
        case THEN:
          Term.set(Term.ForeColor.CYAN);
          break;
          
        default:
          Term.restoreColor();
      }
      
      while (length < token.getPosition().getStartCol()) {
        System.out.print(" ");
        length++;
      }
      
      System.out.print(token);
      length += token.toString().length();
    }
    
    System.out.println();
    Term.restoreColor();
  }
  
  private final Interpreter mInterpreter;
  
  private final BufferedReader mInput;
  private boolean mFirstLine = true;
  private String mLine = "";
  private int mPosition = 0;
}
