#!/usr/bin/python

# Runs the language tests.
from os import listdir
from os.path import abspath, dirname, isdir, join, realpath, relpath
import re
from subprocess import Popen, PIPE

MAGPIE_DIR = dirname(dirname(realpath(__file__)))
TEST_DIR = join(MAGPIE_DIR, 'test')
# TODO(bob): Support other platforms and configurations.
MAGPIE_APP = join(MAGPIE_DIR, 'build', 'Debug', 'magpie')

EXPECT_PATTERN = re.compile(r'// expect: (.*)')

class color:
    GREEN = '\033[32m'
    RED = '\033[31m'
    DEFAULT = '\033[0m'

def walk(dir, callback):
    """ Walks [dir], and executes [callback] on each file. """
    dir = abspath(dir)
    for file in [file for file in listdir(dir) if not file in [".",".."]]:
        nfile = join(dir, file)
        callback(nfile)
        if isdir(nfile):
            walk(nfile, callback)


def run_test(path):
    # Make a nice short path relative to the working directory.
    path = relpath(path)

    # Read the test and parse out the expectations.
    expect_output = []
    with open(path, 'r') as file:
        for line in file:
            match = EXPECT_PATTERN.search(line)
            if match:
                expect_output.append(match.group(1))

    # Invoke magpie and run the test.
    proc = Popen([MAGPIE_APP, path], stdout=PIPE, stderr=PIPE)
    (out, err) = proc.communicate()

    fails = []

    # Validate the output.
    expect_index = 0
    for line in out.split():
        if expect_index >= len(expect_output):
            fails.append('Got output "{0}" when none was expected.'.
                format(line))
        elif expect_output[expect_index] != line:
            fails.append('Expected output "{0}" and got "{1}".'.
                format(expect_output[expect_index], line))
        expect_index += 1

    while expect_index < len(expect_output):
        fails.append('Missing expected output "{0}".'.
            format(expect_output[expect_index]))
        expect_index += 1

    # Validate the error output.
    # TODO(bob): Implement. Allow tests to specify which lines compile errors
    # should occur on a parse those out of the stderr.

    # Validate the return code.
    # TODO(bob): Allow tests to specify expected return code.
    expect_return = 0
    if proc.returncode != expect_return:
        fails.append('Expected return code {0} and got {1}.'.
            format(expect_return, proc.returncode))

    # Display the results.
    if len(fails) == 0:
        print color.GREEN + 'PASS' + color.DEFAULT + ': ' + path
    else:
        print color.RED + 'FAIL' + color.DEFAULT + ': ' + path
        for fail in fails:
            print '     ', fail

walk(TEST_DIR, run_test)
