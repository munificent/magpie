import re
from pygments import highlight
from pygments.lexers import PythonLexer
from pygments.formatters import HtmlFormatter

from pygments.lexer import RegexLexer
from pygments.token import *

class MagpieLexer(RegexLexer):
    name = 'Magpie'
    aliases = ['mag', 'magpie']
    filenames = ['*.mag']

    flags = re.MULTILINE | re.DOTALL

    tokens = {
        'root': [
            (r'\s+', Text),
            
            # keywords
            (r'(and|break|case|class|def|do|else|end|extend|false|fn|for|if|'
             r'interface|let|match|namespace|nothing|or|return|shared|'
             r'struct|then|union|using|this|true|typeof|var|while)\b', Keyword),
            
            # keywords
            (r'(\<\-|\-\>|\.)', Keyword),
             
            (r'[,()\\\[\]{}]', Punctuation),
            
            # comments
            (r'//[^\n]*?\n', Comment.Single),
            (r'/\*.*?\*/', Comment.Multiline),

            # user-defined names
            (r'[a-zA-Z_][a-zA-Z_0-9`~!$%^&*\-=+\\|/?<>]*', Name),
            #(r'[`~!$%^&*\-=+\\|/?<>][a-zA-Z_0-9`~!$%^&*\-=+\\|/?<>]*', Operator),
            
            # literals
            
            # numbers
            (r'(\d+\.\d*|\.\d+|\d+)[eE][+-]?\d+[lL]?', Number.Float),
            (r'(\d+\.\d*|\.\d+|\d+[fF])[fF]?', Number.Float),
            (r'0x[0-9a-fA-F]+[Ll]?', Number.Hex),
            (r'\d+[Ll]?', Number.Integer),
            
            # strings
            (r'L?"', String, 'string'),
        ],
        'string': [
            (r'"', String, '#pop'),
            (r'\\([\\abfnrtv"\']|x[a-fA-F0-9]{2,4}|[0-7]{1,3})', String.Escape),
            (r'[^\\"\n]+', String), # all other characters
            (r'\\\n', String), # line continuation
            (r'\\', String), # stray backslash
        ],
    }
    
    # bob: hack. i know i want it to guess magpie
    def analyse_text(text):
        return 1.0