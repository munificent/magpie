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
            (r'(and|as|break|case|catch|def|defclass|definfix|do|else|end|fn|for|'
             r'if|import|in|is|match|or|return|then|throw|val|var|with|while)\b', Keyword),

            # keywords
            # (r'(=)', Keyword),

            (r'[,()\\\[\]{}]', Punctuation),

            # comments
            (r'//[^\n]*?\n', Comment.Single),
            (r'/\*.*?\*/', Comment.Multiline),

            # names and operators
            (r'[~!$%^&*\-=+\\|/?<>\.]+', Name),
            (r'[a-zA-Z_.][a-zA-Z_.0-9]+', Name),

            # built-in names
            (r'(true|false|nothing)\b', Name.Builtin),
            (r'(this|it)\b', Name.Builtin.Pseudo),

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
