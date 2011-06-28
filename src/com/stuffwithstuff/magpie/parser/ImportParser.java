package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ImportDeclaration;

public class ImportParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    
    String scheme = null;
    if (parser.match(TokenType.FIELD)) {
      scheme = parser.last(1).getString();
    }
    
    // Parse the module name.
    String module = parser.consume(TokenType.NAME).getString();
    
    // Parse the prefix, if any.
    String prefix = null;
    if (parser.match("as")) {
      prefix = parser.consume(TokenType.NAME).getString();
    }
    
    // Parse the declarations, if any.
    List<ImportDeclaration> declarations = new ArrayList<ImportDeclaration>();
    boolean isOnly = false;
    
    if (parser.match("with")) {
      if (parser.match("only")) isOnly = true;
      
      parser.consume(TokenType.LINE);
      
      while (!parser.match("end")) {
        // TODO(bob): "excluding".
        
        boolean export = parser.match("export");
        
        String name = parser.consume(TokenType.NAME).getString();
        String rename = null;
        if (parser.match("as")) {
          rename = parser.consume(TokenType.NAME).getString();
        }
        
        parser.consume(TokenType.LINE);
        declarations.add(new ImportDeclaration(export, name, rename));
      }
    }
    
    /*
        import foo.bar
    
    Import foo/bar.mag, same as before.
    
        import foo.bar as blah
    
    Import foo/bar.mag, but prefix everything with "blah.".
    
        import foo.bar with
            excluding blah
            bang as bong
        end
    
    Import foo/bar.mag, but not "blah". Import "bang" from it but renamed to "bong".
    
        import foo.bar with only
            blah
            bang
        end
    
    Just import "blah" and "bang" from foo/bar.mag, and nothing else.
    
        import foo.bar with only blah
    
    Shorthand for the above if you only want to import a single name.
    
        import foo.bar with
            export bang
        end
    
    Import everything from foo/bar.mag, including "bang", and then
    re-export "bang" from the importing module. In general, "export" and
    "as" can be combined and used anywhere a name can appear, so this is
    valid too:

    import foo.bar with only export bang as bong 
     */
    
    
    return Expr.import_(span.end(), scheme, module, prefix, isOnly, declarations);
  }
}
