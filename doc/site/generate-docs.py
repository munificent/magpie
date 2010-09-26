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
        info = [relpath, get_info(f)]
        if os.path.isdir(dirname):
            info.append(build_nav(dirname))
        nav.append(info)
    # sort by the indices
    nav.sort(key=lambda i: i[1]['index'])
    return nav

def get_info(path):
    basename = os.path.basename(path)
    basename = basename.split('.')[0]

    # read the markdown file
    info = dict()
    info['title'] = path
    info['index'] = path
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

def make_nav_html(nav, thisfile):
    html = ""
    thisdir = os.path.dirname(thisfile)
    for item in nav:
        link = item[1]['title']
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
            if not stripped.startswith('^'):
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
        out.write("""
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
        <head>
        <meta http-equiv="Content-type" content="text/html;charset=UTF-8" />
        <title>Magpie: {0}</title>
        <link rel="stylesheet" type="text/css"
          href="http://fonts.googleapis.com/css?family=Reenie+Beanie|Droid+Sans+Mono">
        <link rel="stylesheet" type="text/css" href="{1}" />
        </head>
        <body>""".format(title, stylerel))
        
        roothref = os.path.relpath('html/index.html', dir)
        navhtml = make_nav_html(nav, basename + '.html')
        html = markdown.markdown(contents, ['def_list', 'codehilite'])

        out.write("""
        <h1>{0}</h1>
        <table>
          <tr class="header">
            <td><h2 class="subhead"><a href="{1}">Magpie:</a></h2></td>
            <td><h2>{2}</h2></td>
          </tr>
          <tr>
            <td>
              <div class="nav">
                <ul>{3}</ul>
              </div>
            </td>
            <td>
              <div class="content">{4}<div>
            </td>
          </tr>
        </table>
        </div>
        </body>
        </html>
        """.format(breadcrumb(basename), roothref, title, navhtml, html))


def breadcrumb(path):
    # handle the top level page a little specially
    if path == 'index': return '&nbsp;'
    
    outdir = 'html/' + os.path.dirname(path)

    #rootpath = os.path.relpath('html/index.html', outdir)
    #breadcrumb = '<a href="{0}">Magpie Docs</a>'.format(rootpath)
    breadcrumb = ''
    
    # add links to the parent directories
    dir = ''
    for part in os.path.dirname(path).split('/'):
        if part == '': break
        dir = os.path.join(dir, part)
        title = get_title('markdown/{0}.markdown'.format(dir))
        relpath = os.path.relpath('html/{0}.html'.format(dir), outdir)
        breadcrumb += ' &rsaquo; <a href="{0}">{1}</a>'.format(relpath, title)
        
    # include the name of the page itself
    title = get_title('markdown/{0}.markdown'.format(path))
    breadcrumb += ' &rsaquo; ' + title
    
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
    if os.path.splitext(path)[1] == '.markdown':
        count += 1
        format_file(path, nav)


# clean out the output directory
if os.path.isdir('html'):
    shutil.rmtree('html')

# copy over the static content
shutil.copytree('static', 'html')

nav = build_nav('markdown')
walk('markdown', do_format)
print 'Generated', count, 'HTML files.'
