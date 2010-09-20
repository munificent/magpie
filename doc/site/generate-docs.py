import glob
import markdown
import os
import shutil
from datetime import datetime

from magpie import MagpieLexer

def build_nav(path):
    nav = []
    for f in glob.iglob(path + '/*.markdown'):
        dirname = os.path.splitext(f)[0]
        # get the relative path to the html file
        relpath = os.path.relpath(dirname, 'markdown') + '.html'
        info = [relpath, get_title(f)]
        if os.path.isdir(dirname):
            info.append(build_nav(dirname))
        nav.append(info)
    return nav

def get_title(path):
    basename = os.path.basename(path)
    basename = basename.split('.')[0]
    
    # read the markdown file
    with open(path, 'r') as input:
        # read each line, ignoring everything but the special codes
        for line in input:
            stripped = line.lstrip()
            if stripped.startswith('^'):
                command,_,args = stripped.rstrip('\n').lstrip('^').partition(' ')
                args = args.strip()
                
                if command == 'title':
                    title = args
                else:
                    print "UNKNOWN COMMAND:", command, args
    return title

def make_nav_html(nav, thisfile):
    html = ""
    thisdir = os.path.dirname(thisfile)
    for item in nav:
        link = item[1]
        linkpath = os.path.relpath(item[0], thisdir)
        if thisfile != item[0]:
            link = '<a href={0}>{1}</a>'.format(linkpath, link)
        else:
            link = '<strong>{0}</strong>'.format(link)
        
        html = html + '<li>{0}'.format(link)
        if len(item) == 3:
            # has children
            html += '\n<ul>' + make_nav_html(item[2], thisfile)
            html += '\n</ul>\n'
        else:
            html += '</li>\n'
    return html

def html_path(pattern):
    return 'html/' + pattern + '.html'
    
def format_file(path, nav):
    basename = os.path.relpath(path, 'markdown')
    basename = basename.split('.')[0]
    
    title = '<unknown title>'
    
    # read the markdown file and preprocess it
    contents = ''
    with open(path, 'r') as input:
        # read each line, preprocessing the special codes
        for line in input:
            stripped = line.lstrip()
            indentation = line[:len(line) - len(stripped)]
            
            if stripped.startswith('^'):
                command,_,args = stripped.rstrip('\n').lstrip('^').partition(' ')
                args = args.strip()
                
                if command == 'title':
                    title = args
                else:
                    print "UNKNOWN COMMAND:", command, args
                    
            else:
                contents = contents + line
    
    modified = datetime.fromtimestamp(os.path.getmtime(path))
    mod_str = modified.strftime('%B %d, %Y')
    
    # create the directory if needed
    dir = 'html/' + os.path.dirname(basename)
    if not os.path.isdir(dir):
        os.makedirs(dir)
    
    stylerel = os.path.relpath('html/style.css', dir)
    
    # write the html output
    with open('html/{0}.html'.format(basename), 'w') as out:
        header = """
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
        <head>
        <meta http-equiv="Content-type" content="text/html;charset=UTF-8" />
        <title>The Magpie Programming Language</title>
        <link rel="stylesheet" type="text/css"
          href="http://fonts.googleapis.com/css?family=Reenie+Beanie|Droid+Sans+Mono">
        <link rel="stylesheet" type="text/css" href="{0}" />
        </head>
        <body>
        <h1>&rsaquo; <a href="#">Magpie Guide</a> &rsaquo; <a href="#">Basic Syntax</a></h1>
        <div class="nav">
        <ul>
        """.format(stylerel)
        out.write(header)
        
        out.write(make_nav_html(nav, basename + '.html'))
        
        header2 = """
          </ul>
        </div>
        <div class="content">
        """
        out.write(header2)
            
        # title
        out.write('<h2>{0}</h2>\n'.format(title))
            
        # content
        html = markdown.markdown(contents, ['def_list', 'codehilite'])
        out.write(html)

        footer = """
        </div>
        </body>
        </html>
        """
        out.write(footer)
        
    print "converted", basename


# clean out the output directory
if os.path.isdir('html'):
    shutil.rmtree('html')

# copy over the static content
shutil.copytree('static', 'html')

nav = build_nav('markdown')

def walk(dir, callback):
    """ walks a directory, and executes a callback on each file """
    dir = os.path.abspath(dir)
    for file in [file for file in os.listdir(dir) if not file in [".",".."]]:
        nfile = os.path.join(dir, file)
        callback(nfile)
        if os.path.isdir(nfile):
            walk(nfile, callback)

def do_format(path):
    if os.path.splitext(path)[1] == '.markdown':
        format_file(path, nav)

walk('markdown', do_format)

# process each markdown file
#for f in glob.iglob('markdown/*.markdown'):
#    format_file(f, nav)