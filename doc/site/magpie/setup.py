"""
A Pygments lexer for Magpie.
"""
from setuptools import setup

__author__ = 'Robert Nystrom'

setup(
    name='Magpie',
    version='1.0',
    description=__doc__,
    author=__author__,
    packages=['magpie'],
    entry_points='''
    [pygments.lexers]
    magpielexer = magpie:MagpieLexer
    '''
)