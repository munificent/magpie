package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.Term.ForeColor;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringCharacterReader;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.parser.Lexer;

/**
 * Provides a string of characters by reading them from the user a line at a
 * time, as requested.
 */
public class NiceReplCharacterReader extends ReplCharacterReader {
  public NiceReplCharacterReader(Interpreter interpreter) {
    mInterpreter = interpreter;
  }

  @Override
  protected void showPrompt(String prompt) {
    Term.set(Term.ForeColor.GRAY);
    System.out.print(prompt);
    Term.set(Term.ForeColor.WHITE);
  }
  
  @Override
  protected void afterReadLine(String prompt, String line) {
    try {
      Lexer lexer = new Lexer(new StringCharacterReader("", line));
  
      Term.moveUp();
  
      // Redraw the prompt.
      Term.set(Term.ForeColor.GRAY);
      System.out.print(prompt);
      
      while (true) {
        Token token = lexer.readToken();
        if (token.getType() == TokenType.EOF) break;

        switch (token.getType()) {
        case LEFT_PAREN:
        case RIGHT_PAREN:
        case LEFT_BRACKET:
        case RIGHT_BRACKET:
        case LEFT_BRACE:
        case RIGHT_BRACE:
        case BACKTICK:
        case COMMA:
        case EQUALS:
          Term.set(Term.ForeColor.GRAY);
          break;
        
        // Identifiers.
        case NAME:
          if (token.isKeyword("this") || token.isKeyword("nothing")) {
            // special identifiers
            Term.set(ForeColor.LIGHT_BLUE);
          } else if (mInterpreter.getGrammar().isReserved(token.getString())) {
            Term.set(ForeColor.CYAN);
          } else {
            Term.set(ForeColor.WHITE);
          }
          break;
          
        case FIELD:
          Term.set(Term.ForeColor.GRAY);
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
         
        case BLOCK_COMMENT:
        case DOC_COMMENT:
        case LINE_COMMENT:
          Term.set(Term.ForeColor.GRAY);
          break;
          
        default:
          Term.restoreColor();
        }
        
        System.out.print(token.getText());
      }
    } catch(ParseException ex) {
      // Do nothing, just eat it.
    }
    
    System.out.println();
    Term.restoreColor();
  }
  
  private final Interpreter mInterpreter;
}
