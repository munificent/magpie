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
            (r'(and|async|break|case|catch|def|defclass|do|end|else|false|fn|'
             r'for|if|import|in|is|match|not|nothing|or|return|then|throw|true|'
             r'val|var|with|while|xor)\b', Keyword),

            (r'[,()\\\[\]{}]', Punctuation),

            # comments
            (r'/\*', Comment.Multiline, 'comment'),
            (r'//.*?$', Comment.Single),

            # names and operators
            (r'[~!$%^&*\-=+\\|/?<>\.]+', Operator),
            (r'[a-zA-Z_.][a-zA-Z_.0-9]+', Name),

            # literals

            # numbers
            (r'(\d+\.\d*|\.\d+|\d+)[eE][+-]?\d+[lL]?', Number.Float),
            (r'(\d+\.\d*|\.\d+|\d+[fF])[fF]?', Number.Float),
            (r'0x[0-9a-fA-F]+[Ll]?', Number.Hex),
            (r'\d+[Ll]?', Number.Integer),

            # strings
            (r"L?'", String.Char, 'character'),
            (r'L?"', String, 'string'),
        ],
        'comment': [
            (r'\*/', Comment.Multiline, '#pop'),
            (r'/\*', Comment.Multiline, '#push'),
            (r'.', Comment.Multiline), # all other characters
        ],
        'character': [
            (r"'", String.Char, '#pop'),
            (r'\\([\\abfnrtv"\']|x[a-fA-F0-9]{2,4}|[0-7]{1,3})', String.Escape),
            (r'[^\\"\n]+', String.Char), # all other characters
        ],
        'string': [
            (r'"', String, '#pop'),
            (r'\\([\\abfnrtv"\']|x[a-fA-F0-9]{2,4}|[0-7]{1,3})', String.Escape),
            (r'[^\\"\n]+', String), # all other characters
            (r'\\\n', String), # line continuation
            (r'\\', String), # stray backslash
        ],
    }
