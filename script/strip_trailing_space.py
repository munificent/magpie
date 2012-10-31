#!/usr/bin/python

# Remove trailing whitespace from source files

from os import listdir
from os.path import realpath, abspath, dirname, join, isdir
import fileinput

DIR = dirname(dirname(realpath(__file__)))

def walk(dir, callback):
    """ Walks [dir], and executes [callback] on each file. """
    dir = abspath(dir)
    for file in [file for file in listdir(dir) if not file in [".",".."]]:
        nfile = join(dir, file)
        if isdir(nfile):
            walk(nfile, callback)
        else:
            callback(nfile)

def strip_trailing_space(nfile):
    if nfile.endswith(".mag") or nfile.endswith(".py") or nfile.endswith(".cpp") or nfile.endswith(".h") or nfile.endswith(".java"):
        for line in fileinput.FileInput(nfile, inplace=1):
            line = line.rstrip()
            print line

walk(DIR, strip_trailing_space)

print "DONE"








