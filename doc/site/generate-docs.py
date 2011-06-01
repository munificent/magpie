#!/usr/bin/python

import glob
import markdown
import os
import shutil
from datetime import datetime

from magpie import MagpieLexer

def get_info(path):
    basename = os.path.basename(path)
    basename = basename.split('.')[0]

    # read the markdown file
    info = dict()
    info['title'] = path

    with open(path, 'r') as input:
        # read each line, ignoring everything but the special codes
        for line in input:
            stripped = line.lstrip()
            if stripped.startswith('^'):
                command,_,args = stripped.rstrip('\n').lstrip('^').partition(' ')
                args = args.strip()

                info[command] = args
    return info

def get_title(path):
    info = get_info(path)
    return info['title']

def html_path(pattern):
    return 'html/' + pattern + '.html'

def format_file(path):
    basename = os.path.relpath(path, 'markdown')
    basename = basename.split('.')[0]

    info = get_info(path)
    title = info['title']

    # read the markdown file and preprocess it
    contents = ''
    with open(path, 'r') as input:
        # read each line, preprocessing the special codes
        for line in input:
            stripped = line.lstrip()
            indentation = line[:len(line) - len(stripped)]

            # ignore commands
            if stripped.startswith('^'):
                pass
            elif stripped.startswith('#'):
                # build the page navigation from the headers
                index = stripped.find(" ")
                headertype = stripped[:index]
                header = stripped[index:].strip()
                anchor = header.lower().replace(' ', '-')
                anchor = anchor.translate(None, '.?!:/')

                # add an anchor to the header
                contents += indentation + headertype
                contents += '<a href="#{0}" name="{0}">{1}</a>\n'.format(anchor, header)
            else:
                contents = contents + line

    modified = datetime.fromtimestamp(os.path.getmtime(path))
    mod_str = modified.strftime('%B %d, %Y')

    # create the directory if needed
    dir = 'html/' + os.path.dirname(basename)
    if not os.path.isdir(dir):
        os.makedirs(dir)

    root = os.path.relpath('html/', dir) + '/'
    if root == './':
        root = ''

    html = markdown.markdown(contents, ['def_list', 'codehilite'])

    # determine the next and previous pages based on the ordering list
#    if not basename in ordering:
#        print 'Don\'t have an ordering for', basename
#    this_index = ordering.index(basename)
#    prev = ordering[(this_index + len(ordering) - 1) % len(ordering)]
#    next = ordering[(this_index + 1) % len(ordering)]
#
#    prev = '<a href="{0}{1}.html">&laquo; Previous</a>'.format(root, prev)
#    next = '<a href="{0}{1}.html">Next &raquo;</a>'.format(root, next)

    # load the template page
    template = open('template.html', 'r').read()

    # insert the content
    template = template.replace('$(title)', title)
#    template = template.replace('$(prev)', prev)
#    template = template.replace('$(next)', next)
#    template = template.replace('$(breadcrumb)', breadcrumb(basename))
    template = template.replace('$(root)', root)
    template = template.replace('$(content)', html)

    with open('html/{0}.html'.format(basename), 'w') as out:
        out.write(template)

def breadcrumb(path):
    # handle the top level page a little specially
    if path == 'index': return 'Magpie Guide'

    outdir = 'html/' + os.path.dirname(path)

    rootpath = os.path.relpath('html/index.html', outdir)
    breadcrumb = '<a href="{0}">Magpie Guide</a>'.format(rootpath)

    # add links to the parent directories
    dir = ''
    for part in os.path.dirname(path).split('/'):
        if part == '': break
        dir = os.path.join(dir, part)
        title = get_title('markdown/{0}.md'.format(dir))
        relpath = os.path.relpath('html/{0}.html'.format(dir), outdir)
        breadcrumb += ' / <a href="{0}">{1}</a>'.format(relpath, title)

    # include the name of the page itself
    title = get_title('markdown/{0}.md'.format(path))
    breadcrumb += ' / ' + title

    return breadcrumb


def walk(dir, callback):
    """ walks a directory, and executes a callback on each file """
    dir = os.path.abspath(dir)
    for file in [file for file in os.listdir(dir) if not file in [".",".."]]:
        nfile = os.path.join(dir, file)
        callback(nfile)
        if os.path.isdir(nfile):
            walk(nfile, callback)

count = 0
def do_format(path):
    global count
    if os.path.splitext(path)[1] == '.md':
        count += 1
        format_file(path)


# clean out the output directory
if os.path.isdir('html'):
    shutil.rmtree('html')

# copy over the static content
shutil.copytree('static', 'html')

def strip_newline(line):
    return line.rstrip()

#ordering = open("order.txt", "r").readlines()
#ordering = map(strip_newline, ordering)

walk('markdown', do_format)
print 'Generated', count, 'HTML files.'
