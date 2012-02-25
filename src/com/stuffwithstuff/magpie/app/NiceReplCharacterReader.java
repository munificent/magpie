package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.Repl;
import com.stuffwithstuff.magpie.app.Term.ForeColor;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringReader;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.parser.Lexer;

/**
 * Provides a string of characters by reading them from the user a line at a
 * time, as requested.
 */
public class NiceReplCharacterReader extends ReplReader {
  public NiceReplCharacterReader(Repl repl) {
    super(repl);
  }
  
  @Override
  protected void showPrompt(String prompt) {
    Term.set(Term.ForeColor.GRAY);
    System.out.print(prompt);
    Term.restoreColor();
  }
  
  @Override
  protected void afterReadLine(Repl repl, String prompt, String line) {
    try {
      Lexer lexer = new Lexer(new StringReader("", line));
  
      Term.moveUp();
  
      // TODO(bob): Now that there are token types for reserved words and
      // operators, should handle them here.
      
      // Redraw the prompt.
      Term.set(Term.ForeColor.GRAY);
      System.out.print(prompt);
      Term.restoreColor();

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
        case EQ:
          Term.set(Term.ForeColor.GRAY);
          break;
        
        // Identifiers.
        case NAME:
          if (token.isKeyword("this") ||
              token.isKeyword("nothing") ||
              token.isKeyword("it")) {
            // special identifiers
            Term.set(ForeColor.LIGHT_BLUE);
            // TODO(bob): Fix this once we have token types for the reserved words.
            /*
          } else if (repl.isKeyword(token.getString())) {
            Term.set(ForeColor.CYAN);
            */
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
}
