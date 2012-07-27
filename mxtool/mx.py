#!/usr/bin/python
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------
#

r"""
mx is a command line tool inspired by mvn (http://maven.apache.org/)
and hg (http://mercurial.selenic.com/). It includes a mechanism for
managing the dependencies between a set of projects (like Maven)
as well as making it simple to run commands
(like hg is the interface to the Mercurial commands).

The organizing principle of mx is a project suite. A suite is a directory
containing one or more projects. It's not coincidental that this closely
matches the layout of one or more projects in a Mercurial repository.
The configuration information for a suite lives in an 'mx' sub-directory
at the top level of the suite.

When launched, mx treats the current working directory as a suite.
This is the primary suite. All other suites are called included suites.

The configuration files (i.e. in the 'mx' sub-directory) of a suite are:

  projects
      Defines the projects and libraries in the suite and the
      dependencies between them.

  commands.py
      Suite specific extensions to the commands available to mx.

  includes
      Other suites to be loaded. This is recursive. Each
      line in an includes file is a path to a suite directory.

  env
      A set of environment variable definitions. These override any
      existing environment variables.

The includes and env files are typically not put under version control
as they usually contain local file-system paths.

The projects file is like the pom.xml file from Maven except that
it is a properties file (not XML). Each non-comment line
in the file specifies an attribute of a project or library. The main
difference between a project and a library is that the former contains
source code built by the mx tool where as the latter is an external
dependency. The format of the projects file is

Library specification format:

    library@<name>@<prop>=<value>

Built-in library properties (* = required):

   *path
        The file system path for the library to appear on a class path.

    urls
        A comma separated list of URLs from which the library (jar) can
        be downloaded and saved in the location specified by 'path'.

    optional
        If "true" then this library will be omitted from a class path
        if it doesn't exist on the file system and no URLs are specified.

Project specification format:

    project@<name>@<prop>=<value>

The name of a project also denotes the directory it is in.

Built-in project properties (* = required):

    subDir
        The sub-directory of the suite in which the project directory is
        contained. If not specified, the project directory is directly
        under the suite directory.

   *sourceDirs
        A comma separated list of source directory names (relative to
        the project directory).

    dependencies
        A comma separated list of the libraries and project the project
        depends upon (transitive dependencies should be omitted).

    checkstyle
        The project whose Checkstyle configuration
        (i.e. <project>/.checkstyle_checks.xml) is used.

    native
        "true" if the project is native.

    javaCompliance
        The minimum JDK version (format: x.y) to which the project's
        sources comply (required for non-native projects).

Other properties can be specified for projects and libraries for use
by extension commands.

Property values can use environment variables with Bash syntax (e.g. ${HOME}).
"""

import sys, os, errno, time, subprocess, shlex, types, urllib2, contextlib, StringIO, zipfile, signal, xml.sax.saxutils, tempfile
import shutil, fnmatch, re, xml.dom.minidom
from collections import Callable
from threading import Thread
from argparse import ArgumentParser, REMAINDER
from os.path import join, basename, dirname, exists, getmtime, isabs, expandvars, isdir, isfile

DEFAULT_JAVA_ARGS = '-ea -Xss2m -Xmx1g'

_projects = dict()
_libs = dict()
_suites = dict()
_mainSuite = None
_opts = None
_java = None

"""
A dependency is a library or project specified in a suite.
"""
class Dependency:
    def __init__(self, suite, name):
        self.name = name
        self.suite = suite

    def __str__(self):
        return self.name

    def __eq__(self, other):
        return self.name == other.name

    def __ne__(self, other):
        return self.name != other.name

    def __hash__(self):
        return hash(self.name)

    def isLibrary(self):
        return isinstance(self, Library)

class Project(Dependency):
    def __init__(self, suite, name, srcDirs, deps, javaCompliance, dir):
        Dependency.__init__(self, suite, name)
        self.srcDirs = srcDirs
        self.deps = deps
        self.checkstyleProj = name
        self.javaCompliance = JavaCompliance(javaCompliance) if javaCompliance is not None else None
        self.native = False
        self.dir = dir

        # Create directories for projects that don't yet exist
        if not exists(dir):
            os.mkdir(dir)
        for s in self.source_dirs():
            if not exists(s):
                os.mkdir(s)

    def all_deps(self, deps, includeLibs, includeSelf=True):
        """
        Add the transitive set of dependencies for this project, including
        libraries if 'includeLibs' is true, to the 'deps' list.
        """
        if self in deps:
            return deps
        for name in self.deps:
            assert name != self.name
            dep = _libs.get(name, None)
            if dep is not None:
                if includeLibs and not dep in deps:
                    deps.append(dep)
            else:
                dep = project(name)
                if not dep in deps:
                    dep.all_deps(deps, includeLibs)
        if not self in deps and includeSelf:
            deps.append(self)
        return deps

    def _compute_max_dep_distances(self, name, distances, dist):
        currentDist = distances.get(name);
        if currentDist is None or currentDist < dist:
            distances[name] = dist
            p = project(name, False)
            if p is not None:
                for dep in p.deps:
                    self._compute_max_dep_distances(dep, distances, dist + 1)

    def canonical_deps(self):
        """
        Get the dependencies of this project that are not recursive (i.e. cannot be reached
        via other dependencies).
        """
        distances = dict()
        result = set()
        self._compute_max_dep_distances(self.name, distances, 0)
        for n,d in distances.iteritems():
            assert d > 0 or n == self.name
            if d == 1:
                result.add(n)

        if len(result) == len(self.deps) and frozenset(self.deps) == result:
            return self.deps
        return result;

    def max_depth(self):
        """
        Get the maximum canonical distance between this project and its most distant dependency.
        """
        distances = dict()
        self._compute_max_dep_distances(self.name, distances, 0)
        return max(distances.values())

    def source_dirs(self):
        """
        Get the directories in which the sources of this project are found.
        """
        return [join(self.dir, s) for s in self.srcDirs]

    def output_dir(self):
        """
        Get the directory in which the class files of this project are found/placed.
        """
        if self.native:
            return None
        return join(self.dir, 'bin')

    def jasmin_output_dir(self):
        """
        Get the directory in which the Jasmin assembled class files of this project are found/placed.
        """
        if self.native:
            return None
        return join(self.dir, 'jasmin_classes')

    def append_to_classpath(self, cp, resolve):
        if not self.native:
            cp.append(self.output_dir())

    def find_classes_with_matching_source_line(self, pkgRoot, function, includeInnerClasses=False):
        """
        Scan the sources of this project for Java source files containing a line for which
        'function' returns true. The fully qualified class name of each existing class
        corresponding to a matched source file is returned in a list.
        """
        classes = []
        pkgDecl = re.compile(r"^package\s+([a-zA-Z_][\w\.]*)\s*;$")
        for srcDir in self.source_dirs():
            outputDir = self.output_dir()
            for root, _, files in os.walk(srcDir):
                for name in files:
                    if name.endswith('.java') and name != 'package-info.java':
                        matchFound = False
                        with open(join(root, name)) as f:
                            pkg = None
                            for line in f:
                                if line.startswith("package "):
                                    match = pkgDecl.match(line)
                                    if match:
                                        pkg = match.group(1)
                                if function(line.strip()):
                                    matchFound = True
                                if pkg and matchFound:
                                    break

                        if matchFound:
                            basename = name[:-len('.java')]
                            assert pkg is not None
                            if pkgRoot is None or pkg.startswith(pkgRoot):
                                pkgOutputDir = join(outputDir, pkg.replace('.', os.path.sep))
                                for e in os.listdir(pkgOutputDir):
                                    if includeInnerClasses:
                                        if e.endswith('.class') and (e.startswith(basename) or e.startswith(basename + '$')):
                                            classes.append(pkg + '.' + e[:-len('.class')])
                                    elif e == basename + '.class':
                                        classes.append(pkg + '.' + basename)
        return classes


class Library(Dependency):
    def __init__(self, suite, name, path, mustExist, urls):
        Dependency.__init__(self, suite, name)
        self.path = path.replace('/', os.sep)
        self.urls = urls
        self.mustExist = mustExist

    def get_path(self, resolve):
        path = self.path
        if not isabs(path):
            path = join(self.suite.dir, path)
        if resolve and self.mustExist and not exists(path):
            assert not len(self.urls) == 0, 'cannot find required library  ' + self.name + " " + path;
            print('Downloading ' + self.name + ' from ' + str(self.urls))
            download(path, self.urls)

        return path

    def append_to_classpath(self, cp, resolve):
        path = self.get_path(resolve)
        if exists(path) or not resolve:
            cp.append(path)

class Suite:
    def __init__(self, dir, primary):
        self.dir = dir
        self.projects = []
        self.libs = []
        self.includes = []
        self.commands = None
        self.primary = primary
        mxDir = join(dir, 'mx')
        self._load_env(mxDir)
        self._load_commands(mxDir)
        self._load_includes(mxDir)

    def _load_projects(self, mxDir):
        libsMap = dict()
        projsMap = dict()
        projectsFile = join(mxDir, 'projects')
        if not exists(projectsFile):
            return
        with open(projectsFile) as f:
            for line in f:
                line = line.strip()
                if len(line) != 0 and line[0] != '#':
                    key, value = line.split('=', 1)

                    parts = key.split('@')

                    if len(parts) == 2:
                        pass
                    if len(parts) != 3:
                        abort('Property name does not have 3 parts separated by "@": ' + key)
                    kind, name, attr = parts
                    if kind == 'project':
                        m = projsMap
                    elif kind == 'library':
                        m = libsMap
                    else:
                        abort('Property name does not start with "project@" or "library@": ' + key)

                    attrs = m.get(name)
                    if attrs is None:
                        attrs = dict()
                        m[name] = attrs
                    value = expandvars_in_property(value)
                    attrs[attr] = value

        def pop_list(attrs, name):
            v = attrs.pop(name, None)
            if v is None or len(v.strip()) == 0:
                return []
            return [n.strip() for n in v.split(',')]

        for name, attrs in projsMap.iteritems():
            srcDirs = pop_list(attrs, 'sourceDirs')
            deps = pop_list(attrs, 'dependencies')
            javaCompliance = attrs.pop('javaCompliance', None)
            subDir = attrs.pop('subDir', None);
            if subDir is None:
                dir = join(self.dir, name)
            else:
                dir = join(self.dir, subDir, name)
            p = Project(self, name, srcDirs, deps, javaCompliance, dir)
            p.checkstyleProj = attrs.pop('checkstyle', name)
            p.native = attrs.pop('native', '') == 'true'
            if not p.native and p.javaCompliance is None:
                abort('javaCompliance property required for non-native project ' + name)
            p.__dict__.update(attrs)
            self.projects.append(p)

        for name, attrs in libsMap.iteritems():
            path = attrs.pop('path')
            mustExist = attrs.pop('optional', 'false') != 'true'
            urls = pop_list(attrs, 'urls')
            l = Library(self, name, path, mustExist, urls)
            l.__dict__.update(attrs)
            self.libs.append(l)

    def _load_commands(self, mxDir):
        commands = join(mxDir, 'commands.py')
        if exists(commands):
            # temporarily extend the Python path
            sys.path.insert(0, mxDir)
            mod = __import__('commands')

            sys.modules[join(mxDir, 'commands')] = sys.modules.pop('commands')

            # revert the Python path
            del sys.path[0]

            if not hasattr(mod, 'mx_init'):
                abort(commands + ' must define an mx_init(env) function')
            if hasattr(mod, 'mx_post_parse_cmd_line'):
                self.mx_post_parse_cmd_line = mod.mx_post_parse_cmd_line

            mod.mx_init()
            self.commands = mod

    def _load_includes(self, mxDir):
        includes = join(mxDir, 'includes')
        if exists(includes):
            with open(includes) as f:
                for line in f:
                    include = expandvars_in_property(line.strip())
                    self.includes.append(include)
                    _loadSuite(include, False)

    def _load_env(self, mxDir):
        e = join(mxDir, 'env')
        if exists(e):
            with open(e) as f:
                for line in f:
                    line = line.strip()
                    if len(line) != 0 and line[0] != '#':
                        key, value = line.split('=', 1)
                        os.environ[key.strip()] = expandvars_in_property(value.strip())

    def _post_init(self, opts):
        mxDir = join(self.dir, 'mx')
        self._load_projects(mxDir)
        if hasattr(self, 'mx_post_parse_cmd_line'):
            self.mx_post_parse_cmd_line(opts)
        for p in self.projects:
            existing = _projects.get(p.name)
            if existing is not None:
                abort('cannot override project  ' + p.name + ' in ' + p.dir + " with project of the same name in  " + existing.dir)
            _projects[p.name] = p
        for l in self.libs:
            existing = _libs.get(l.name)
            if existing is not None:
                abort('cannot redefine library  ' + l.name)
            _libs[l.name] = l

class XMLElement(xml.dom.minidom.Element):
    def writexml(self, writer, indent="", addindent="", newl=""):
        writer.write(indent+"<" + self.tagName)

        attrs = self._get_attributes()
        a_names = attrs.keys()
        a_names.sort()

        for a_name in a_names:
            writer.write(" %s=\"" % a_name)
            xml.dom.minidom._write_data(writer, attrs[a_name].value)
            writer.write("\"")
        if self.childNodes:
            if not self.ownerDocument.padTextNodeWithoutSiblings and len(self.childNodes) == 1 and isinstance(self.childNodes[0], xml.dom.minidom.Text):
                # if the only child of an Element node is a Text node, then the
                # text is printed without any indentation or new line padding
                writer.write(">")
                self.childNodes[0].writexml(writer)
                writer.write("</%s>%s" % (self.tagName,newl))
            else:
                writer.write(">%s"%(newl))
                for node in self.childNodes:
                    node.writexml(writer,indent+addindent,addindent,newl)
                writer.write("%s</%s>%s" % (indent,self.tagName,newl))
        else:
            writer.write("/>%s"%(newl))

class XMLDoc(xml.dom.minidom.Document):

    def __init__(self):
        xml.dom.minidom.Document.__init__(self)
        self.current = self
        self.padTextNodeWithoutSiblings = False

    def createElement(self, tagName):
        # overwritten to create XMLElement
        e = XMLElement(tagName)
        e.ownerDocument = self
        return e

    def open(self, tag, attributes={}, data=None):
        element = self.createElement(tag)
        for key, value in attributes.items():
            element.setAttribute(key, value)
        self.current.appendChild(element)
        self.current = element
        if data is not None:
            element.appendChild(self.createTextNode(data))
        return self

    def close(self, tag):
        assert self.current != self
        assert tag == self.current.tagName, str(tag) + ' != ' + self.current.tagName
        self.current = self.current.parentNode
        return self

    def element(self, tag, attributes={}, data=None):
        return self.open(tag, attributes, data).close(tag)

    def xml(self, indent='', newl='', escape=False):
        assert self.current == self
        result = self.toprettyxml(indent, newl, encoding="UTF-8")
        if escape:
            entities = { '"':  "&quot;", "'":  "&apos;", '\n': '&#10;' }
            result = xml.sax.saxutils.escape(result, entities)
        return result

def get_os():
    """
    Get a canonical form of sys.platform.
    """
    if sys.platform.startswith('darwin'):
        return 'darwin'
    elif sys.platform.startswith('linux'):
        return 'linux'
    elif sys.platform.startswith('sunos'):
        return 'solaris'
    elif sys.platform.startswith('win32') or sys.platform.startswith('cygwin'):
        return 'windows'
    else:
        abort('Unknown operating system ' + sys.platform)

def _loadSuite(dir, primary=False):
    mxDir = join(dir, 'mx')
    if not exists(mxDir) or not isdir(mxDir):
        return None
    if not _suites.has_key(dir):
        suite = Suite(dir, primary)
        _suites[dir] = suite
        return suite

def suites():
    """
    Get the list of all loaded suites.
    """
    return _suites.values()

def projects():
    """
    Get the list of all loaded projects.
    """
    return _projects.values()

def project(name, fatalIfMissing=True):
    """
    Get the project for a given name. This will abort if the named project does
    not exist and 'fatalIfMissing' is true.
    """
    p = _projects.get(name)
    if p is None and fatalIfMissing:
        abort('project named ' + name + ' not found')
    return p

def library(name, fatalIfMissing=True):
    """
    Gets the library for a given name. This will abort if the named library does
    not exist and 'fatalIfMissing' is true.
    """
    l = _libs.get(name)
    if l is None and fatalIfMissing:
        abort('library named ' + name + ' not found')
    return l

def _as_classpath(deps, resolve):
    cp = []
    if _opts.cp_prefix is not None:
        cp = [_opts.cp_prefix]
    for d in deps:
        d.append_to_classpath(cp, resolve)
    if _opts.cp_suffix is not None:
        cp += [_opts.cp_suffix]
    return os.pathsep.join(cp)

def classpath(names=None, resolve=True, includeSelf=True, includeBootClasspath=False):
    """
    Get the class path for a list of given projects, resolving each entry in the
    path (e.g. downloading a missing library) if 'resolve' is true.
    """
    if names is None:
        result = _as_classpath(sorted_deps(includeLibs=True), resolve)
    else:
        deps = []
        if isinstance(names, types.StringTypes):
            project(names).all_deps(deps, True, includeSelf)
        else:
            for n in names:
                project(n).all_deps(deps, True, includeSelf)
        result = _as_classpath(deps, resolve)
    if includeBootClasspath:
        result = os.pathsep.join([java().bootclasspath(), result])
    return result

def classpath_walk(names=None, resolve=True, includeSelf=True, includeBootClasspath=False):
    """
    Walks the resources available in a given classpath, yielding a tuple for each resource
    where the first member of the tuple is a directory path or ZipFile object for a
    classpath entry and the second member is the qualified path of the resource relative
    to the classpath entry.
    """
    cp = classpath(names, resolve, includeSelf, includeBootClasspath)
    for entry in cp.split(os.pathsep):
        if not exists(entry):
            continue
        if isdir(entry):
            for root, dirs, files in os.walk(entry):
                for d in dirs:
                    entryPath = join(root[len(entry) + 1:], d)
                    yield entry, entryPath
                for f in files:
                    entryPath = join(root[len(entry) + 1:], f)
                    yield entry, entryPath
        elif entry.endswith('.jar') or entry.endswith('.zip'):
            with zipfile.ZipFile(entry, 'r') as zf:
                for zi in zf.infolist():
                    entryPath = zi.filename
                    yield zf, entryPath

def sorted_deps(projectNames=None, includeLibs=False):
    """
    Gets projects and libraries sorted such that dependencies
    are before the projects that depend on them. Unless 'includeLibs' is
    true, libraries are omitted from the result.
    """
    deps = []
    if projectNames is None:
        projects = _projects.values()
    else:
        projects = [project(name) for name in projectNames]

    for p in projects:
        p.all_deps(deps, includeLibs)
    return deps

class ArgParser(ArgumentParser):

    # Override parent to append the list of available commands
    def format_help(self):
        return ArgumentParser.format_help(self) + _format_commands()


    def __init__(self):
        self.java_initialized = False
        ArgumentParser.__init__(self, prog='mx')

        self.add_argument('-v', action='store_true', dest='verbose', help='enable verbose output')
        self.add_argument('-V', action='store_true', dest='very_verbose', help='enable very verbose output')
        self.add_argument('--dbg', type=int, dest='java_dbg_port', help='make Java processes wait on <port> for a debugger', metavar='<port>')
        self.add_argument('-d', action='store_const', const=8000, dest='java_dbg_port', help='alias for "-dbg 8000"')
        self.add_argument('--cp-pfx', dest='cp_prefix', help='class path prefix', metavar='<arg>')
        self.add_argument('--cp-sfx', dest='cp_suffix', help='class path suffix', metavar='<arg>')
        self.add_argument('--J', dest='java_args', help='Java VM arguments (e.g. --J @-dsa)', metavar='@<args>', default=DEFAULT_JAVA_ARGS)
        self.add_argument('--Jp', action='append', dest='java_args_pfx', help='prefix Java VM arguments (e.g. --Jp @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--Ja', action='append', dest='java_args_sfx', help='suffix Java VM arguments (e.g. --Ja @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--user-home', help='users home directory', metavar='<path>', default=os.path.expanduser('~'))
        self.add_argument('--java-home', help='JDK installation directory (must be JDK 6 or later)', metavar='<path>')
        if get_os() != 'windows':
            # Time outs are (currently) implemented with Unix specific functionality
            self.add_argument('--timeout', help='Timeout (in seconds) for command', type=int, default=0, metavar='<secs>')
            self.add_argument('--ptimeout', help='Timeout (in seconds) for subprocesses', type=int, default=0, metavar='<secs>')

    def _parse_cmd_line(self, args=None):
        if args is None:
            args = sys.argv[1:]

        self.add_argument('commandAndArgs', nargs=REMAINDER, metavar='command args...')

        opts = self.parse_args()

        # Give the timeout options a default value to avoid the need for hasattr() tests
        opts.__dict__.setdefault('timeout', 0)
        opts.__dict__.setdefault('ptimeout', 0)

        if opts.very_verbose:
            opts.verbose = True

        if opts.java_home is None:
            opts.java_home = os.environ.get('JAVA_HOME')

        if opts.java_home is None or opts.java_home == '':
            abort('Could not find Java home. Use --java-home option or ensure JAVA_HOME environment variable is set.')

        if opts.user_home is None or opts.user_home == '':
            abort('Could not find user home. Use --user-home option or ensure HOME environment variable is set.')

        os.environ['JAVA_HOME'] = opts.java_home
        os.environ['HOME'] = opts.user_home

        commandAndArgs = opts.__dict__.pop('commandAndArgs')
        return opts, commandAndArgs

def _format_commands():
    msg = '\navailable commands:\n\n'
    for cmd in sorted(commands.iterkeys()):
        c, _ = commands[cmd][:2]
        doc = c.__doc__
        if doc is None:
            doc = ''
        msg += ' {0:<20} {1}\n'.format(cmd, doc.split('\n', 1)[0])
    return msg + '\n'

def java():
    """
    Get a JavaConfig object containing Java commands launch details.
    """
    assert _java is not None
    return _java

def run_java(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return run(java().format_cmd(args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def _kill_process_group(pid):
    pgid = os.getpgid(pid)
    try:
        os.killpg(pgid, signal.SIGKILL)
        return True
    except:
        log('Error killing subprocess ' + str(pgid) + ': ' + str(sys.exc_info()[1]))
        return False

def _waitWithTimeout(process, args, timeout):
    def _waitpid(pid):
        while True:
            try:
                return os.waitpid(pid, os.WNOHANG)
            except OSError, e:
                if e.errno == errno.EINTR:
                    continue
                raise

    def _returncode(status):
        if os.WIFSIGNALED(status):
            return -os.WTERMSIG(status)
        elif os.WIFEXITED(status):
            return os.WEXITSTATUS(status)
        else:
            # Should never happen
            raise RuntimeError("Unknown child exit status!")

    end = time.time() + timeout
    delay = 0.0005
    while True:
        (pid, status) = _waitpid(process.pid)
        if pid == process.pid:
            return _returncode(status)
        remaining = end - time.time()
        if remaining <= 0:
            abort('Process timed out after {0} seconds: {1}'.format(timeout, ' '.join(args)))
        delay = min(delay * 2, remaining, .05)
        time.sleep(delay)

# Makes the current subprocess accessible to the abort() function
# This is a tuple of the Popen object and args.
_currentSubprocess = None

def waitOn(p):
    if get_os() == 'windows':
        # on windows use a poll loop, otherwise signal does not get handled
        retcode = None
        while retcode == None:
            retcode = p.poll()
            time.sleep(0.05)
    else:
        retcode = p.wait()
    return retcode

def run(args, nonZeroIsFatal=True, out=None, err=None, cwd=None, timeout=None):
    """
    Run a command in a subprocess, wait for it to complete and return the exit status of the process.
    If the exit status is non-zero and `nonZeroIsFatal` is true, then mx is exited with
    the same exit status.
    Each line of the standard output and error streams of the subprocess are redirected to
    out and err if they are callable objects.
    """

    assert isinstance(args, types.ListType), "'args' must be a list: " + str(args)
    for arg in args:
        assert isinstance(arg, types.StringTypes), 'argument is not a string: ' + str(arg)

    if _opts.verbose:
        if _opts.very_verbose:
            log('Environment variables:')
            for key in sorted(os.environ.keys()):
                log('    ' + key + '=' + os.environ[key])
        log(' '.join(args))

    if timeout is None and _opts.ptimeout != 0:
        timeout = _opts.ptimeout

    global _currentSubprocess

    try:
        # On Unix, the new subprocess should be in a separate group so that a timeout alarm
        # can use os.killpg() to kill the whole subprocess group
        preexec_fn = None
        creationflags = 0
        if get_os() == 'windows':
            creationflags = subprocess.CREATE_NEW_PROCESS_GROUP
        else:
            preexec_fn = os.setsid

        if not callable(out) and not callable(err) and timeout is None:
            # The preexec_fn=os.setsid
            p = subprocess.Popen(args, cwd=cwd, preexec_fn=preexec_fn, creationflags=creationflags)
            _currentSubprocess = (p, args)
            retcode = waitOn(p)
        else:
            def redirect(stream, f):
                for line in iter(stream.readline, ''):
                    f(line)
                stream.close()
            stdout=out if not callable(out) else subprocess.PIPE
            stderr=err if not callable(err) else subprocess.PIPE
            p = subprocess.Popen(args, cwd=cwd, stdout=stdout, stderr=stderr, preexec_fn=preexec_fn, creationflags=creationflags)
            _currentSubprocess = (p, args)
            if callable(out):
                t = Thread(target=redirect, args=(p.stdout, out))
                t.daemon = True # thread dies with the program
                t.start()
            if callable(err):
                t = Thread(target=redirect, args=(p.stderr, err))
                t.daemon = True # thread dies with the program
                t.start()
            if timeout is None or timeout == 0:
                retcode = waitOn(p)
            else:
                if get_os() == 'windows':
                    abort('Use of timeout not (yet) supported on Windows')
                retcode = _waitWithTimeout(p, args, timeout)
    except OSError as e:
        log('Error executing \'' + ' '.join(args) + '\': ' + str(e))
        if _opts.verbose:
            raise e
        abort(e.errno)
    except KeyboardInterrupt:
        abort(1)
    finally:
        _currentSubprocess = None

    if retcode and nonZeroIsFatal:
        if _opts.verbose:
            if _opts.very_verbose:
                raise subprocess.CalledProcessError(retcode, ' '.join(args))
            else:
                log('[exit code: ' + str(retcode)+ ']')
        abort(retcode)

    return retcode

def exe_suffix(name):
    """
    Gets the platform specific suffix for an executable
    """
    if get_os() == 'windows':
        return name + '.exe'
    return name

def lib_suffix(name):
    """
    Gets the platform specific suffix for a library
    """
    os = get_os();
    if os == 'windows':
        return name + '.dll'
    if os == 'linux' or os == 'solaris':
        return name + '.so'
    if os == 'darwin':
        return name + '.dylib'
    return name

"""
A JavaCompliance simplifies comparing Java compliance values extracted from a JDK version string.
"""
class JavaCompliance:
    def __init__(self, ver):
        m = re.match('1\.(\d+).*', ver)
        assert m is not None, 'not a recognized version string: ' + ver
        self.value = int(m.group(1))

    def __str__ (self):
        return '1.' + str(self.value)

    def __cmp__ (self, other):
        if isinstance(other, types.StringType):
            other = JavaCompliance(other)

        return cmp(self.value, other.value)

"""
A JavaConfig object encapsulates info on how Java commands are run.
"""
class JavaConfig:
    def __init__(self, opts):
        self.jdk = opts.java_home
        self.debug_port = opts.java_dbg_port
        self.java =  exe_suffix(join(self.jdk, 'bin', 'java'))
        self.javac = exe_suffix(join(self.jdk, 'bin', 'javac'))
        self.javap = exe_suffix(join(self.jdk, 'bin', 'javap'))
        self.javadoc = exe_suffix(join(self.jdk, 'bin', 'javadoc'))
        self._bootclasspath = None

        if not exists(self.java):
            abort('Java launcher derived from JAVA_HOME does not exist: ' + self.java)

        def delAtAndSplit(s):
            return shlex.split(s.lstrip('@'))

        self.java_args = delAtAndSplit(_opts.java_args)
        self.java_args_pfx = sum(map(delAtAndSplit, _opts.java_args_pfx), [])
        self.java_args_sfx = sum(map(delAtAndSplit, _opts.java_args_sfx), [])

        # Prepend the -d64 VM option only if the java command supports it
        try:
            output = subprocess.check_output([self.java, '-d64', '-version'], stderr=subprocess.STDOUT)
            self.java_args = ['-d64'] + self.java_args
        except subprocess.CalledProcessError as e:
            try:
                output = subprocess.check_output([self.java, '-version'], stderr=subprocess.STDOUT)
            except subprocess.CalledProcessError as e:
                print e.output
                abort(e.returncode)

        output = output.split()
        assert output[1] == 'version'
        self.version = output[2].strip('"')
        self.javaCompliance = JavaCompliance(self.version)

        if self.debug_port is not None:
            self.java_args += ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=' + str(self.debug_port)]

    def format_cmd(self, args):
        return [self.java] + self.java_args_pfx + self.java_args + self.java_args_sfx + args

    def bootclasspath(self):
        if self._bootclasspath is None:
            tmpDir = tempfile.mkdtemp()
            try:
                src = join(tmpDir, 'bootclasspath.java')
                with open(src, 'w') as fp:
                    print >> fp, """
public class bootclasspath {
    public static void main(String[] args) {
        String s = System.getProperty("sun.boot.class.path");
        if (s != null) {
            System.out.println(s);
        }
    }
}"""
                subprocess.check_call([self.javac, '-d', tmpDir, src])
                self._bootclasspath = subprocess.check_output([self.java, '-cp', tmpDir, 'bootclasspath'])
            finally:
                shutil.rmtree(tmpDir)
        return self._bootclasspath

def check_get_env(key):
    """
    Gets an environment variable, aborting with a useful message if it is not set.
    """
    value = get_env(key)
    if value is None:
        abort('Required environment variable ' + key + ' must be set')
    return value

def get_env(key, default=None):
    """
    Gets an environment variable.
    """
    value = os.environ.get(key, default)
    return value

def log(msg=None):
    """
    Write a message to the console.
    All script output goes through this method thus allowing a subclass
    to redirect it.
    """
    if msg is None:
        print
    else:
        print msg

def expand_project_in_class_path_arg(cpArg):
    cp = []
    for part in cpArg.split(os.pathsep):
        if part.startswith('@'):
            cp += classpath(part[1:]).split(os.pathsep)
        else:
            cp.append(part)
    return os.pathsep.join(cp)

def expand_project_in_args(args):
    for i in range(len(args)):
        if args[i] == '-cp' or args[i] == '-classpath':
            if i + 1 < len(args):
                args[i + 1] = expand_project_in_class_path_arg(args[i + 1])
            return


def gmake_cmd():
    for a in ['make', 'gmake', 'gnumake']:
        try:
            output = subprocess.check_output([a, '--version'])
            if 'GNU' in output:
                return a;
        except:
            pass
    abort('Could not find a GNU make executable on the current path.')

def expandvars_in_property(value):
        result = expandvars(value)
        if '$' in result or '%' in result:
            abort('Property contains an undefined environment variable: ' + value)
        return result


def abort(codeOrMessage):
    """
    Aborts the program with a SystemExit exception.
    If 'codeOrMessage' is a plain integer, it specifies the system exit status;
    if it is None, the exit status is zero; if it has another type (such as a string),
    the object's value is printed and the exit status is one.
    """

    #import traceback
    #traceback.print_stack()
    currentSubprocess = _currentSubprocess
    if currentSubprocess is not None:
        p, _ = currentSubprocess
        if get_os() == 'windows':
            p.kill()
        else:
            _kill_process_group(p.pid)

    raise SystemExit(codeOrMessage)

def download(path, urls, verbose=False):
    """
    Attempts to downloads content for each URL in a list, stopping after the first successful download.
    If the content cannot be retrieved from any URL, the program is aborted. The downloaded content
    is written to the file indicated by 'path'.
    """
    d = dirname(path)
    if d != '' and not exists(d):
        os.makedirs(d)

    # Try it with the Java tool first since it can show a progress counter
    myDir = dirname(__file__)

    javaSource = join(myDir, 'URLConnectionDownload.java')
    javaClass = join(myDir, 'URLConnectionDownload.class')
    if not exists(javaClass) or getmtime(javaClass) < getmtime(javaSource):
        subprocess.check_call([java().javac, '-d', myDir, javaSource])
    if run([java().java, '-cp', myDir, 'URLConnectionDownload', path] + urls) == 0:
        return

    def url_open(url):
        userAgent = 'Mozilla/5.0 (compatible)'
        headers = { 'User-Agent' : userAgent }
        req = urllib2.Request(url, headers=headers)
        return urllib2.urlopen(req);

    for url in urls:
        try:
            if (verbose):
                log('Downloading ' + url + ' to ' + path)
            if url.startswith('zip:') or url.startswith('jar:'):
                i = url.find('!/')
                if i == -1:
                    abort('Zip or jar URL does not contain "!/": ' + url)
                url, _, entry = url[len('zip:'):].partition('!/')
                with contextlib.closing(url_open(url)) as f:
                    data = f.read()
                    zipdata = StringIO.StringIO(f.read())

                zf = zipfile.ZipFile(zipdata, 'r')
                data = zf.read(entry)
                with open(path, 'wb') as f:
                    f.write(data)
            else:
                with contextlib.closing(url_open(url)) as f:
                    data = f.read()
                with open(path, 'wb') as f:
                    f.write(data)
            return
        except IOError as e:
            log('Error reading from ' + url + ': ' + str(e))
        except zipfile.BadZipfile as e:
            log('Error in zip file downloaded from ' + url + ': ' + str(e))

    abort('Could not download to ' + path + ' from any of the following URLs:\n\n    ' +
              '\n    '.join(urls) + '\n\nPlease use a web browser to do the download manually')

def update_file(path, content):
    """
    Updates a file with some given content if the content differs from what's in
    the file already. The return value indicates if the file was updated.
    """
    existed = exists(path)
    try:
        old = None
        if existed:
            with open(path, 'rb') as f:
                old = f.read()

        if old == content:
            return False

        with open(path, 'wb') as f:
            f.write(content)

        log(('modified ' if existed else 'created ') + path)
        return True;
    except IOError as e:
        abort('Error while writing to ' + path + ': ' + str(e));

# Builtin commands

def build(args, parser=None):
    """compile the Java and C sources, linking the latter

    Compile all the Java source code using the appropriate compilers
    and linkers for the various source code types."""

    suppliedParser = parser is not None
    if not suppliedParser:
        parser = ArgumentParser(prog='mx build')

    javaCompliance = java().javaCompliance

    defaultEcjPath = join(_mainSuite.dir, 'mx', 'ecj.jar')

    parser = parser if parser is not None else ArgumentParser(prog='mx build')
    parser.add_argument('-f', action='store_true', dest='force', help='force build (disables timestamp checking)')
    parser.add_argument('-c', action='store_true', dest='clean', help='removes existing build output')
    parser.add_argument('--source', dest='compliance', help='Java compliance level', default=str(javaCompliance))
    parser.add_argument('--Wapi', action='store_true', dest='warnAPI', help='show warnings about using internal APIs')
    parser.add_argument('--projects', action='store', help='comma separated projects to build (omit to build all projects)')
    parser.add_argument('--no-java', action='store_false', dest='java', help='do not build Java projects')
    parser.add_argument('--no-native', action='store_false', dest='native', help='do not build native projects')
    parser.add_argument('--jdt', help='Eclipse installation or path to ecj.jar for using the Eclipse batch compiler (default: ' + defaultEcjPath + ')', default=defaultEcjPath, metavar='<path>')

    if suppliedParser:
        parser.add_argument('remainder', nargs=REMAINDER, metavar='...')

    args = parser.parse_args(args)

    jdtJar = None
    if args.jdt is not None:
        if args.jdt.endswith('.jar'):
            jdtJar=args.jdt
            if not exists(jdtJar) and os.path.abspath(jdtJar) == os.path.abspath(defaultEcjPath):
                # Silently ignore JDT if default location is used but not ecj.jar exists there
                jdtJar = None
        elif isdir(args.jdt):
            plugins = join(args.jdt, 'plugins')
            choices = [f for f in os.listdir(plugins) if fnmatch.fnmatch(f, 'org.eclipse.jdt.core_*.jar')]
            if len(choices) != 0:
                jdtJar = join(plugins, sorted(choices, reverse=True)[0])

    built = set()

    projects = None
    if args.projects is not None:
        projects = args.projects.split(',')

    for p in sorted_deps(projects):
        if p.native:
            if args.native:
                log('Calling GNU make {0}...'.format(p.dir))

                if args.clean:
                    run([gmake_cmd(), 'clean'], cwd=p.dir)

                run([gmake_cmd()], cwd=p.dir)
                built.add(p.name)
            continue
        else:
            if not args.java:
                continue

        # skip building this Java project if its Java compliance level is "higher" than the configured JDK
        if javaCompliance < p.javaCompliance:
            log('Excluding {0} from build (Java compliance level {1} required)'.format(p.name, p.javaCompliance))
            continue


        outputDir = p.output_dir()
        if exists(outputDir):
            if args.clean:
                log('Cleaning {0}...'.format(outputDir))
                shutil.rmtree(outputDir)
                os.mkdir(outputDir)
        else:
            os.mkdir(outputDir)

        cp = classpath(p.name, includeSelf=True)
        sourceDirs = p.source_dirs()
        mustBuild = args.force
        if not mustBuild:
            for dep in p.all_deps([], False):
                if dep.name in built:
                    mustBuild = True

        jasminAvailable = None
        javafilelist = []
        for sourceDir in sourceDirs:
            for root, _, files in os.walk(sourceDir):
                javafiles = [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
                javafilelist += javafiles

                # Copy all non Java resources or assemble Jasmin files
                nonjavafilelist = [join(root, name) for name in files if not name.endswith('.java')]
                for src in nonjavafilelist:
                    if src.endswith('.jasm'):
                        className = None
                        with open(src) as f:
                            for line in f:
                                if line.startswith('.class '):
                                    className = line.split()[-1]
                                    break

                        if className is not None:
                            jasminOutputDir = p.jasmin_output_dir()
                            classFile = join(jasminOutputDir, className.replace('/', os.sep) + '.class')
                            if exists(dirname(classFile)) and (not exists(classFile) or os.path.getmtime(classFile) < os.path.getmtime(src)):
                                if jasminAvailable is None:
                                    try:
                                        with open(os.devnull) as devnull:
                                            subprocess.call('jasmin', stdout=devnull, stderr=subprocess.STDOUT)
                                        jasminAvailable = True
                                    except OSError:
                                        jasminAvailable = False

                                if jasminAvailable:
                                    log('Assembling Jasmin file ' + src)
                                    run(['jasmin', '-d', jasminOutputDir, src])
                                else:
                                    log('The jasmin executable could not be found - skipping ' + src)
                                    with file(classFile, 'a'):
                                        os.utime(classFile, None)

                        else:
                            log('could not file .class directive in Jasmin source: ' + src)
                    else:
                        dst = join(outputDir, src[len(sourceDir) + 1:])
                        if exists(dirname(dst)) and (not exists(dst) or os.path.getmtime(dst) != os.path.getmtime(src)):
                            shutil.copyfile(src, dst)

                if not mustBuild:
                    for javafile in javafiles:
                        classfile = outputDir + javafile[len(sourceDir):-len('java')] + 'class'
                        if not exists(classfile) or os.path.getmtime(javafile) > os.path.getmtime(classfile):
                            mustBuild = True
                            break

        if not mustBuild:
            log('[all class files for {0} are up to date - skipping]'.format(p.name))
            continue

        if len(javafilelist) == 0:
            log('[no Java sources for {0} - skipping]'.format(p.name))
            continue

        built.add(p.name)

        argfileName = join(p.dir, 'javafilelist.txt')
        argfile = open(argfileName, 'wb')
        argfile.write('\n'.join(javafilelist))
        argfile.close()

        try:
            if jdtJar is None:
                log('Compiling Java sources for {0} with javac...'.format(p.name))
                javacCmd = [java().javac, '-g', '-J-Xmx1g', '-source', args.compliance, '-classpath', cp, '-d', outputDir, '@' + argfile.name]
                if not args.warnAPI:
                    javacCmd.append('-XDignore.symbol.file')
                run(javacCmd)
            else:
                log('Compiling Java sources for {0} with JDT...'.format(p.name))
                jdtArgs = [java().java, '-Xmx1g', '-jar', jdtJar,
                         '-' + args.compliance,
                         '-cp', cp, '-g', '-enableJavadoc',
                         '-d', outputDir]
                jdtProperties = join(p.dir, '.settings', 'org.eclipse.jdt.core.prefs')
                if not exists(jdtProperties):
                    # Try to fix a missing properties file by running eclipseinit
                    eclipseinit([])
                if not exists(jdtProperties):
                    log('JDT properties file {0} not found'.format(jdtProperties))
                else:
                    jdtArgs += ['-properties', jdtProperties]
                jdtArgs.append('@' + argfile.name)
                run(jdtArgs)
        finally:
            os.remove(argfileName)

    if suppliedParser:
        return args
    return None

def canonicalizeprojects(args):
    """process all project files to canonicalize the dependencies

    The exit code of this command reflects how many files were updated."""

    changedFiles = 0
    for s in suites():
        projectsFile = join(s.dir, 'mx', 'projects')
        if not exists(projectsFile):
            continue
        with open(projectsFile) as f:
            out = StringIO.StringIO()
            pattern = re.compile('project@([^@]+)@dependencies=.*')
            for line in f:
                line = line.strip()
                m = pattern.match(line)
                if m is None:
                    out.write(line + '\n')
                else:
                    p = project(m.group(1))
                    out.write('project@' + m.group(1) + '@dependencies=' + ','.join(p.canonical_deps()) + '\n')
            content = out.getvalue()
        if update_file(projectsFile, content):
            changedFiles += 1
    return changedFiles;

def checkstyle(args):
    """run Checkstyle on the Java sources

   Run Checkstyle over the Java sources. Any errors or warnings
   produced by Checkstyle result in a non-zero exit code.

If no projects are given, then all Java projects are checked."""

    for p in sorted_deps():
        if p.native:
            continue
        sourceDirs = p.source_dirs()
        dotCheckstyle = join(p.dir, '.checkstyle')

        if not exists(dotCheckstyle):
            continue

        for sourceDir in sourceDirs:
            javafilelist = []
            for root, _, files in os.walk(sourceDir):
                javafilelist += [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
            if len(javafilelist) == 0:
                log('[no Java sources in {0} - skipping]'.format(sourceDir))
                continue

            timestampFile = join(p.suite.dir, 'mx', 'checkstyle-timestamps', sourceDir[len(p.suite.dir) + 1:].replace(os.sep, '_') + '.timestamp')
            if not exists(dirname(timestampFile)):
                os.makedirs(dirname(timestampFile))
            mustCheck = False
            if exists(timestampFile):
                timestamp = os.path.getmtime(timestampFile)
                for f in javafilelist:
                    if os.path.getmtime(f) > timestamp:
                        mustCheck = True
                        break
            else:
                mustCheck = True

            if not mustCheck:
                log('[all Java sources in {0} already checked - skipping]'.format(sourceDir))
                continue

            if exists(timestampFile):
                os.utime(timestampFile, None)
            else:
                file(timestampFile, 'a')

            dotCheckstyleXML = xml.dom.minidom.parse(dotCheckstyle)
            localCheckConfig = dotCheckstyleXML.getElementsByTagName('local-check-config')[0]
            configLocation = localCheckConfig.getAttribute('location')
            configType = localCheckConfig.getAttribute('type')
            if configType == 'project':
                # Eclipse plugin "Project Relative Configuration" format:
                #
                #  '/<project_name>/<suffix>'
                #
                if configLocation.startswith('/'):
                    name, _, suffix = configLocation.lstrip('/').partition('/')
                    config = join(project(name).dir, suffix)
                else:
                    config = join(p.dir, configLocation)
            else:
                log('[unknown Checkstyle configuration type "' + configType + '" in {0} - skipping]'.format(sourceDir))
                continue

            exclude = join(p.dir, '.checkstyle.exclude')

            if exists(exclude):
                with open(exclude) as f:
                    # Convert patterns to OS separators
                    patterns = [name.rstrip().replace('/', os.sep) for name in f.readlines()]
                def match(name):
                    for p in patterns:
                        if p in name:
                            if _opts.verbose:
                                log('excluding: ' + name)
                            return True
                    return False

                javafilelist = [name for name in javafilelist if not match(name)]

            auditfileName = join(p.dir, 'checkstyleOutput.txt')
            log('Running Checkstyle on {0} using {1}...'.format(sourceDir, config))

            try:

                # Checkstyle is unable to read the filenames to process from a file, and the
                # CreateProcess function on Windows limits the length of a command line to
                # 32,768 characters (http://msdn.microsoft.com/en-us/library/ms682425%28VS.85%29.aspx)
                # so calling Checkstyle must be done in batches.
                while len(javafilelist) != 0:
                    i = 0
                    size = 0
                    while i < len(javafilelist):
                        s = len(javafilelist[i]) + 1
                        if (size + s < 30000):
                            size += s
                            i += 1
                        else:
                            break

                    batch = javafilelist[:i]
                    javafilelist = javafilelist[i:]
                    try:
                        run_java(['-Xmx1g', '-jar', library('CHECKSTYLE').get_path(True), '-c', config, '-o', auditfileName] + batch)
                    finally:
                        if exists(auditfileName):
                            with open(auditfileName) as f:
                                warnings = [line.strip() for line in f if 'warning:' in line]
                                if len(warnings) != 0:
                                    map(log, warnings)
                                    return 1
            finally:
                if exists(auditfileName):
                    os.unlink(auditfileName)
    return 0

def clean(args, parser=None):
    """remove all class files, images, and executables

    Removes all files created by a build, including Java class files, executables, and
    generated images.
    """

    suppliedParser = parser is not None

    parser = parser if suppliedParser else ArgumentParser(prog='mx build');
    parser.add_argument('--no-native', action='store_false', dest='native', help='do not clean native projects')
    parser.add_argument('--no-java', action='store_false', dest='java', help='do not clean Java projects')

    args = parser.parse_args(args)

    for p in projects():
        if p.native:
            if args.native:
                run([gmake_cmd(), '-C', p.dir, 'clean'])
        else:
            if args.java:
                outputDir = p.output_dir()
                if outputDir != '' and exists(outputDir):
                    log('Removing {0}...'.format(outputDir))
                    shutil.rmtree(outputDir)

    if suppliedParser:
        return args

def about(args):
    """show the 'man page' for mx"""
    print __doc__

def help_(args):
    """show help for a given command

With no arguments, print a list of commands and short help for each command.

Given a command name, print help for that command."""
    if len(args) == 0:
        _argParser.print_help()
        return

    name = args[0]
    if not commands.has_key(name):
        _argParser.error('unknown command: ' + name)

    value = commands[name]
    (func, usage) = value[:2]
    doc = func.__doc__
    if len(value) > 2:
        docArgs = value[2:]
        fmtArgs = []
        for d in docArgs:
            if isinstance(d, Callable):
                fmtArgs += [d()]
            else:
                fmtArgs += [str(d)]
        doc = doc.format(*fmtArgs)
    print 'mx {0} {1}\n\n{2}\n'.format(name, usage, doc)

def projectgraph(args, suite=None):
    """create dot graph for project structure ("mx projectgraph | dot -Tpdf -oprojects.pdf")"""

    print 'digraph projects {'
    print 'rankdir=BT;'
    print 'node [shape=rect];'
    for p in projects():
        for dep in p.canonical_deps():
            print '"' + p.name + '"->"' + dep + '"'
    print '}'

def _source_locator_memento(deps):
    slm = XMLDoc()
    slm.open('sourceLookupDirector')
    slm.open('sourceContainers', {'duplicates' : 'false'})

    # Every Java program depends on the JRE
    memento = XMLDoc().element('classpathContainer', {'path' : 'org.eclipse.jdt.launching.JRE_CONTAINER'}).xml()
    slm.element('classpathContainer', {'memento' : memento, 'typeId':'org.eclipse.jdt.launching.sourceContainer.classpathContainer'})

    for dep in deps:
        if dep.isLibrary():
            if hasattr(dep, 'eclipse.container'):
                memento = XMLDoc().element('classpathContainer', {'path' : getattr(dep, 'eclipse.container')}).xml()
                slm.element('classpathContainer', {'memento' : memento, 'typeId':'org.eclipse.jdt.launching.sourceContainer.classpathContainer'})
        else:
            memento = XMLDoc().element('javaProject', {'name' : dep.name}).xml()
            slm.element('container', {'memento' : memento, 'typeId':'org.eclipse.jdt.launching.sourceContainer.javaProject'})

    slm.close('sourceContainers')
    slm.close('sourceLookupDirector')
    return slm

def make_eclipse_attach(hostname, port, name=None, deps=[]):
    """
    Creates an Eclipse launch configuration file for attaching to a Java process.
    """
    slm = _source_locator_memento(deps)
    launch = XMLDoc()
    launch.open('launchConfiguration', {'type' : 'org.eclipse.jdt.launching.remoteJavaApplication'})
    launch.element('stringAttribute', {'key' : 'org.eclipse.debug.core.source_locator_id', 'value' : 'org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector'})
    launch.element('stringAttribute', {'key' : 'org.eclipse.debug.core.source_locator_memento', 'value' : '%s'})
    launch.element('booleanAttribute', {'key' : 'org.eclipse.jdt.launching.ALLOW_TERMINATE', 'value' : 'true'})
    launch.open('mapAttribute', {'key' : 'org.eclipse.jdt.launching.CONNECT_MAP'})
    launch.element('mapEntry', {'key' : 'hostname', 'value' : hostname})
    launch.element('mapEntry', {'key' : 'port', 'value' : port})
    launch.close('mapAttribute')
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.PROJECT_ATTR', 'value' : ''})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.VM_CONNECTOR_ID', 'value' : 'org.eclipse.jdt.launching.socketAttachConnector'})
    launch.close('launchConfiguration')
    launch = launch.xml(newl='\n') % slm.xml(escape=True)

    if name is None:
        name = 'attach-' + hostname + '-' + port
    eclipseLaunches = join('mx', 'eclipse-launches')
    if not exists(eclipseLaunches):
        os.makedirs(eclipseLaunches)
    return update_file(join(eclipseLaunches, name + '.launch'), launch)

def make_eclipse_launch(javaArgs, jre, name=None, deps=[]):
    """
    Creates an Eclipse launch configuration file for running/debugging a Java command.
    """
    mainClass = None
    vmArgs = []
    appArgs = []
    cp = None
    argsCopy = list(reversed(javaArgs))
    while len(argsCopy) != 0:
        a = argsCopy.pop()
        if a == '-jar':
            mainClass = '-jar'
            appArgs = list(reversed(argsCopy))
            break
        if a == '-cp' or a == '-classpath':
            assert len(argsCopy) != 0
            cp = argsCopy.pop()
            vmArgs.append(a)
            vmArgs.append(cp)
        elif a.startswith('-'):
            vmArgs.append(a)
        else:
            mainClass = a
            appArgs = list(reversed(argsCopy))
            break

    if mainClass is None:
        log('Cannot create Eclipse launch configuration without main class or jar file: java ' + ' '.join(javaArgs))
        return False

    if name is None:
        if mainClass == '-jar':
            name = basename(appArgs[0])
            if len(appArgs) > 1 and not appArgs[1].startswith('-'):
                name = name + '_' + appArgs[1]
        else:
            name = mainClass
        name = time.strftime('%Y-%m-%d-%H%M%S_' + name)

    if cp is not None:
        for e in cp.split(os.pathsep):
            for s in suites():
                deps += [p for p in s.projects if e == p.output_dir()]
                deps += [l for l in s.libs if e == l.get_path(False)]

    slm = _source_locator_memento(deps)

    launch = XMLDoc()
    launch.open('launchConfiguration', {'type' : 'org.eclipse.jdt.launching.localJavaApplication'})
    launch.element('stringAttribute', {'key' : 'org.eclipse.debug.core.source_locator_id', 'value' : 'org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector'})
    launch.element('stringAttribute', {'key' : 'org.eclipse.debug.core.source_locator_memento', 'value' : '%s'})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.JRE_CONTAINER', 'value' : 'org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/' + jre})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.MAIN_TYPE', 'value' : mainClass})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.PROGRAM_ARGUMENTS', 'value' : ' '.join(appArgs)})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.PROJECT_ATTR', 'value' : ''})
    launch.element('stringAttribute', {'key' : 'org.eclipse.jdt.launching.VM_ARGUMENTS', 'value' : ' '.join(vmArgs)})
    launch.close('launchConfiguration')
    launch = launch.xml(newl='\n') % slm.xml(escape=True)

    eclipseLaunches = join('mx', 'eclipse-launches')
    if not exists(eclipseLaunches):
        os.makedirs(eclipseLaunches)
    return update_file(join(eclipseLaunches, name + '.launch'), launch)

def eclipseinit(args, suite=None):
    """(re)generate Eclipse project configurations"""

    if suite is None:
        suite = _mainSuite

    for p in projects():
        if p.native:
            continue

        if not exists(p.dir):
            os.makedirs(p.dir)

        out = XMLDoc()
        out.open('classpath')

        for src in p.srcDirs:
            srcDir = join(p.dir, src)
            if not exists(srcDir):
                os.mkdir(srcDir)
            out.element('classpathentry', {'kind' : 'src', 'path' : src})

        # Every Java program depends on the JRE
        out.element('classpathentry', {'kind' : 'con', 'path' : 'org.eclipse.jdt.launching.JRE_CONTAINER'})

        for dep in p.all_deps([], True):
            if dep == p:
                continue;

            if dep.isLibrary():
                if hasattr(dep, 'eclipse.container'):
                    out.element('classpathentry', {'exported' : 'true', 'kind' : 'con', 'path' : getattr(dep, 'eclipse.container')})
                elif hasattr(dep, 'eclipse.project'):
                    out.element('classpathentry', {'combineaccessrules' : 'false', 'exported' : 'true', 'kind' : 'src', 'path' : '/' + getattr(dep, 'eclipse.project')})
                else:
                    path = dep.path
                    if dep.mustExist:
                        dep.get_path(resolve=True)
                        if isabs(path):
                            out.element('classpathentry', {'exported' : 'true', 'kind' : 'lib', 'path' : path})
                        else:
                            # Relative paths for "lib" class path entries have various semantics depending on the Eclipse
                            # version being used (e.g. see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274737) so it's
                            # safest to simply use absolute paths.
                            out.element('classpathentry', {'exported' : 'true', 'kind' : 'lib', 'path' : join(suite.dir, path)})
            else:
                out.element('classpathentry', {'combineaccessrules' : 'false', 'exported' : 'true', 'kind' : 'src', 'path' : '/' + dep.name})

        out.element('classpathentry', {'kind' : 'output', 'path' : getattr(p, 'eclipse.output', 'bin')})
        out.close('classpath')
        update_file(join(p.dir, '.classpath'), out.xml(indent='\t', newl='\n'))

        csConfig = join(project(p.checkstyleProj).dir, '.checkstyle_checks.xml')
        if exists(csConfig):
            out = XMLDoc()

            dotCheckstyle = join(p.dir, ".checkstyle")
            checkstyleConfigPath = '/' + p.checkstyleProj + '/.checkstyle_checks.xml'
            out.open('fileset-config', {'file-format-version' : '1.2.0', 'simple-config' : 'true'})
            out.open('local-check-config', {'name' : 'Checks', 'location' : checkstyleConfigPath, 'type' : 'project', 'description' : ''})
            out.element('additional-data', {'name' : 'protect-config-file', 'value' : 'false'})
            out.close('local-check-config')
            out.open('fileset', {'name' : 'all', 'enabled' : 'true', 'check-config-name' : 'Checks', 'local' : 'true'})
            out.element('file-match-pattern', {'match-pattern' : '.', 'include-pattern' : 'true'})
            out.close('fileset')
            out.open('filter', {'name' : 'all', 'enabled' : 'true', 'check-config-name' : 'Checks', 'local' : 'true'})
            out.element('filter-data', {'value' : 'java'})
            out.close('filter')

            exclude = join(p.dir, '.checkstyle.exclude')
            if exists(exclude):
                out.open('filter', {'name' : 'FilesFromPackage', 'enabled' : 'true'})
                with open(exclude) as f:
                    for line in f:
                        if not line.startswith('#'):
                            line = line.strip()
                            exclDir = join(p.dir, line)
                            assert isdir(exclDir), 'excluded source directory listed in ' + exclude + ' does not exist or is not a directory: ' + exclDir
                        out.element('filter-data', {'value' : line})
                out.close('filter')

            out.close('fileset-config')
            update_file(dotCheckstyle, out.xml(indent='  ', newl='\n'))

        out = XMLDoc()
        out.open('projectDescription')
        out.element('name', data=p.name)
        out.element('comment', data='')
        out.element('projects', data='')
        out.open('buildSpec')
        out.open('buildCommand')
        out.element('name', data='org.eclipse.jdt.core.javabuilder')
        out.element('arguments', data='')
        out.close('buildCommand')
        if exists(csConfig):
            out.open('buildCommand')
            out.element('name', data='net.sf.eclipsecs.core.CheckstyleBuilder')
            out.element('arguments', data='')
            out.close('buildCommand')
        out.close('buildSpec')
        out.open('natures')
        out.element('nature', data='org.eclipse.jdt.core.javanature')
        if exists(csConfig):
            out.element('nature', data='net.sf.eclipsecs.core.CheckstyleNature')
        out.close('natures')
        out.close('projectDescription')
        update_file(join(p.dir, '.project'), out.xml(indent='\t', newl='\n'))

        settingsDir = join(p.dir, ".settings")
        if not exists(settingsDir):
            os.mkdir(settingsDir)

        eclipseSettingsDir = join(suite.dir, 'mx', 'eclipse-settings')
        if exists(eclipseSettingsDir):
            for name in os.listdir(eclipseSettingsDir):
                path = join(eclipseSettingsDir, name)
                if isfile(path):
                    with open(join(eclipseSettingsDir, name)) as f:
                        content = f.read()
                    content = content.replace('${javaCompliance}', str(p.javaCompliance))
                    update_file(join(settingsDir, name), content)

    make_eclipse_attach('localhost', '8000', deps=projects())

def netbeansinit(args, suite=None):
    """(re)generate NetBeans project configurations"""

    if suite is None:
        suite = _mainSuite

    def println(out, obj):
        out.write(str(obj) + '\n')

    updated = False
    for p in projects():
        if p.native:
            continue

        if not exists(join(p.dir, 'nbproject')):
            os.makedirs(join(p.dir, 'nbproject'))

        out = XMLDoc()
        out.open('project', {'name' : p.name, 'default' : 'default', 'basedir' : '.'})
        out.element('description', data='Builds, tests, and runs the project ' + p.name + '.')
        out.element('import', {'file' : 'nbproject/build-impl.xml'})
        out.close('project')
        updated = update_file(join(p.dir, 'build.xml'), out.xml(indent='\t', newl='\n')) or updated

        out = XMLDoc()
        out.open('project', {'xmlns' : 'http://www.netbeans.org/ns/project/1'})
        out.element('type', data='org.netbeans.modules.java.j2seproject')
        out.open('configuration')
        out.open('data', {'xmlns' : 'http://www.netbeans.org/ns/j2se-project/3'})
        out.element('name', data=p.name)
        out.element('explicit-platform', {'explicit-source-supported' : 'true'})
        out.open('source-roots')
        out.element('root', {'id' : 'src.dir'})
        out.close('source-roots')
        out.open('test-roots')
        out.element('root', {'id' : 'test.src.dir'})
        out.close('test-roots')
        out.close('data')

        firstDep = True
        for dep in p.all_deps([], True):
            if dep == p:
                continue;

            if not dep.isLibrary():
                n = dep.name.replace('.', '_')
                if firstDep:
                    out.open('references', {'xmlns' : 'http://www.netbeans.org/ns/ant-project-references/1'})
                    firstDep = False

                out.open('reference')
                out.element('foreign-project', data=n)
                out.element('artifact-type', data='jar')
                out.element('script', data='build.xml')
                out.element('target', data='jar')
                out.element('clean-target', data='clean')
                out.element('id', data='jar')
                out.close('reference')

        if not firstDep:
            out.close('references')

        out.close('configuration')
        out.close('project')
        updated = update_file(join(p.dir, 'nbproject', 'project.xml'), out.xml(indent='    ', newl='\n')) or updated

        out = StringIO.StringIO()
        jdkPlatform = 'JDK_' + java().version

        content = """
annotation.processing.enabled=false
annotation.processing.enabled.in.editor=false
annotation.processing.processors.list=
annotation.processing.run.all.processors=true
annotation.processing.source.output=${build.generated.sources.dir}/ap-source-output
application.title=""" + p.name + """
application.vendor=mx
build.classes.dir=${build.dir}
build.classes.excludes=**/*.java,**/*.form
# This directory is removed when the project is cleaned:
build.dir=bin
build.generated.dir=${build.dir}/generated
build.generated.sources.dir=${build.dir}/generated-sources
# Only compile against the classpath explicitly listed here:
build.sysclasspath=ignore
build.test.classes.dir=${build.dir}/test/classes
build.test.results.dir=${build.dir}/test/results
# Uncomment to specify the preferred debugger connection transport:
#debug.transport=dt_socket
debug.classpath=\\
    ${run.classpath}
debug.test.classpath=\\
    ${run.test.classpath}
# This directory is removed when the project is cleaned:
dist.dir=dist
dist.jar=${dist.dir}/""" + p.name + """.jar
dist.javadoc.dir=${dist.dir}/javadoc
endorsed.classpath=
excludes=
includes=**
jar.compress=false
# Space-separated list of extra javac options
javac.compilerargs=
javac.deprecation=false
javac.processorpath=\\
    ${javac.classpath}
javac.source=1.7
javac.target=1.7
javac.test.classpath=\\
    ${javac.classpath}:\\
    ${build.classes.dir}
javac.test.processorpath=\\
    ${javac.test.classpath}
javadoc.additionalparam=
javadoc.author=false
javadoc.encoding=${source.encoding}
javadoc.noindex=false
javadoc.nonavbar=false
javadoc.notree=false
javadoc.private=false
javadoc.splitindex=true
javadoc.use=true
javadoc.version=false
javadoc.windowtitle=
main.class=
manifest.file=manifest.mf
meta.inf.dir=${src.dir}/META-INF
mkdist.disabled=false
platforms.""" + jdkPlatform + """.home=""" + java().jdk + """
platform.active=""" + jdkPlatform + """
run.classpath=\\
    ${javac.classpath}:\\
    ${build.classes.dir}
# Space-separated list of JVM arguments used when running the project
# (you may also define separate properties like run-sys-prop.name=value instead of -Dname=value
# or test-sys-prop.name=value to set system properties for unit tests):
run.jvmargs=
run.test.classpath=\\
    ${javac.test.classpath}:\\
    ${build.test.classes.dir}
test.src.dir=
source.encoding=UTF-8""".replace(':', os.pathsep).replace('/', os.sep)
        print >> out, content

        mainSrc = True
        for src in p.srcDirs:
            srcDir = join(p.dir, src)
            if not exists(srcDir):
                os.mkdir(srcDir)
            ref = 'file.reference.' + p.name + '-' + src
            print >> out, ref + '=' + src
            if mainSrc:
                print >> out, 'src.dir=${' + ref + '}'
                mainSrc = False
            else:
                print >> out, 'src.' + src + '.dir=${' + ref + '}'

        javacClasspath = []
        for dep in p.all_deps([], True):
            if dep == p:
                continue;

            if dep.isLibrary():
                if not dep.mustExist:
                    continue
                path = dep.get_path(resolve=True)
                if os.sep == '\\':
                    path = path.replace('\\', '\\\\')
                ref = 'file.reference.' + dep.name + '-bin'
                print >> out, ref + '=' + path

            else:
                n = dep.name.replace('.', '_')
                relDepPath = os.path.relpath(dep.dir, p.dir).replace(os.sep, '/')
                ref = 'reference.' + n + '.jar'
                print >> out, 'project.' + n + '=' + relDepPath
                print >> out, ref + '=${project.' + n + '}/dist/' + dep.name + '.jar'

            javacClasspath.append('${' + ref + '}')

        print >> out, 'javac.classpath=\\\n    ' + (os.pathsep + '\\\n    ').join(javacClasspath)


        updated = update_file(join(p.dir, 'nbproject', 'project.properties'), out.getvalue()) or updated
        out.close()

    if updated:
        log('If using NetBeans:')
        log('  1. Ensure that a platform named "JDK ' + java().version + '" is defined (Tools -> Java Platforms)')
        log('  2. Open/create a Project Group for the directory containing the projects (File -> Project Group -> New Group... -> Folder of Projects)')

def ideclean(args, suite=None):
    """remove all Eclipse and NetBeans project configurations"""

    def rm(path):
        if exists(path):
            os.remove(path)

    for p in projects():
        if p.native:
            continue

        shutil.rmtree(join(p.dir, '.settings'), ignore_errors=True)
        shutil.rmtree(join(p.dir, 'nbproject'), ignore_errors=True)
        rm(join(p.dir, '.classpath'))
        rm(join(p.dir, '.project'))
        rm(join(p.dir, 'build.xml'))

def ideinit(args, suite=None):
    """(re)generate Eclipse and NetBeans project configurations"""
    eclipseinit(args, suite)
    netbeansinit(args, suite)

def javadoc(args, parser=None, docDir='javadoc', includeDeps=True, stdDoclet=True):
    """generate javadoc for some/all Java projects"""

    parser = ArgumentParser(prog='mx javadoc') if parser is None else parser
    parser.add_argument('-d', '--base', action='store', help='base directory for output')
    parser.add_argument('--unified', action='store_true', help='put javadoc in a single directory instead of one per project')
    parser.add_argument('--force', action='store_true', help='(re)generate javadoc even if package-list file exists')
    parser.add_argument('--projects', action='store', help='comma separated projects to process (omit to process all projects)')
    parser.add_argument('--Wapi', action='store_true', dest='warnAPI', help='show warnings about using internal APIs')
    parser.add_argument('--argfile', action='store', help='name of file containing extra javadoc options')
    parser.add_argument('--arg', action='append', dest='extra_args', help='extra Javadoc arguments (e.g. --arg @-use)', metavar='@<arg>', default=[])
    parser.add_argument('-m', '--memory', action='store', help='-Xmx value to pass to underlying JVM')
    parser.add_argument('--packages', action='store', help='comma separated packages to process (omit to process all packages)')
    parser.add_argument('--exclude-packages', action='store', help='comma separated packages to exclude')

    args = parser.parse_args(args)

    # build list of projects to be processed
    candidates = sorted_deps()
    if args.projects is not None:
        candidates = [project(name) for name in args.projects.split(',')]

    # optionally restrict packages within a project
    packages = []
    if args.packages is not None:
        packages = [name for name in args.packages.split(',')]

    exclude_packages = []
    if args.exclude_packages is not None:
        exclude_packages = [name for name in args.exclude_packages.split(',')]

    def outDir(p):
        if args.base is None:
            return join(p.dir, docDir)
        return join(args.base, p.name, docDir)

    def check_package_list(p):
        return not exists(join(outDir(p), 'package-list'))

    def assess_candidate(p, projects):
        if p in projects:
            return False
        if args.force or args.unified or check_package_list(p):
            projects.append(p)
            return True
        return False

    projects = []
    for p in candidates:
        if not p.native:
            if includeDeps:
                deps = p.all_deps([], includeLibs=False, includeSelf=False)
                for d in deps:
                    assess_candidate(d, projects)
            if not assess_candidate(p, projects):
                log('[package-list file exists - skipping {0}]'.format(p.name))


    def find_packages(sourceDirs, pkgs=set()):
        for sourceDir in sourceDirs:
            for root, _, files in os.walk(sourceDir):
                if len([name for name in files if name.endswith('.java')]) != 0:
                    pkg = root[len(sourceDir) + 1:].replace(os.sep,'.')
                    if len(packages) == 0 or pkg in packages:
                        if len(exclude_packages) == 0 or not pkg in exclude_packages:
                            pkgs.add(pkg)
        return pkgs

    extraArgs = [a.lstrip('@') for a in args.extra_args]
    if args.argfile is not None:
        extraArgs += ['@' + args.argfile]
    memory = '2g'
    if args.memory is not None:
        memory = args.memory
    memory = '-J-Xmx' + memory

    if not args.unified:
        for p in projects:
            # The project must be built to ensure javadoc can find class files for all referenced classes
            build(['--no-native', '--projects', p.name])

            pkgs = find_packages(p.source_dirs(), set())
            deps = p.all_deps([], includeLibs=False, includeSelf=False)
            links = ['-link', 'http://docs.oracle.com/javase/' + str(p.javaCompliance.value) + '/docs/api/']
            out = outDir(p)
            for d in deps:
                depOut = outDir(d)
                links.append('-link')
                links.append(os.path.relpath(depOut, out))
            cp = classpath(p.name, includeSelf=True)
            sp = os.pathsep.join(p.source_dirs())
            overviewFile = join(p.dir, 'overview.html')
            delOverviewFile = False
            if not exists(overviewFile):
                with open(overviewFile, 'w') as fp:
                    print >> fp, '<html><body>Documentation for the <code>' + p.name + '</code> project.</body></html>'
                delOverviewFile = True
            nowarnAPI = []
            if not args.warnAPI:
                nowarnAPI.append('-XDignore.symbol.file')

            windowTitle = []
            if stdDoclet:
                windowTitle = '-windowtitle', p.name + ' javadoc'
            try:
                log('Generating {2} for {0} in {1}'.format(p.name, out, docDir))
                run([java().javadoc, memory,
                     '-d64',
                     '-classpath', cp,
                     '-quiet',
                     '-d', out,
                     '-overview', overviewFile,
                     '-sourcepath', sp] +
                     links +
                     extraArgs +
                     nowarnAPI +
                     windowTitle +
                     list(pkgs))
                log('Generated {2} for {0} in {1}'.format(p.name, out, docDir))
            finally:
                if delOverviewFile:
                    os.remove(overviewFile)

    else:
        # The projects must be built to ensure javadoc can find class files for all referenced classes
        build(['--no-native'])

        pkgs = set()
        sp = []
        names = []
        for p in projects:
            find_packages(p.source_dirs(), pkgs)
            sp += p.source_dirs()
            names.append(p.name)

        links = ['-link', 'http://docs.oracle.com/javase/' + str(_java.javaCompliance.value) + '/docs/api/']
        out = join(_mainSuite.dir, docDir)
        if args.base is not None:
            out = join(args.base, docDir)
        cp = classpath()
        sp = os.pathsep.join(sp)
        nowarnAPI = []
        if not args.warnAPI:
            nowarnAPI.append('-XDignore.symbol.file')
        log('Generating {2} for {0} in {1}'.format(', '.join(names), out, docDir))
        run([java().javadoc, memory,
             '-classpath', cp,
             '-quiet',
             '-d', out,
             '-sourcepath', sp] +
             links +
             extraArgs +
             nowarnAPI +
             list(pkgs))
        log('Generated {2} for {0} in {1}'.format(', '.join(names), out, docDir))

def site(args):
    """creates a website containing javadoc and the project dependency graph"""

    parser = ArgumentParser(prog='site')
    parser.add_argument('-d', '--base', action='store', help='directory for generated site', required=True, metavar='<dir>')
    parser.add_argument('--name', action='store', help='name of overall documentation', required=True, metavar='<name>')
    parser.add_argument('--overview', action='store', help='path to the overview content for overall documentation', required=True, metavar='<path>')
    parser.add_argument('--projects', action='store', help='comma separated projects to process (omit to process all projects)')
    parser.add_argument('--exclude-packages', action='store', help='comma separated packages to exclude', metavar='<pkgs>')
    parser.add_argument('--dot-output-base', action='store', help='base file name (relative to <dir>/all) for project dependency graph .svg and .jpg files generated by dot (omit to disable dot generation)', metavar='<path>')
    parser.add_argument('--title', action='store', help='value used for -windowtitle and -doctitle javadoc args for overall documentation (default: "<name>")', metavar='<title>')
    args = parser.parse_args(args)

    args.base = os.path.abspath(args.base)
    tmpbase = tempfile.mkdtemp(prefix=basename(args.base) + '.', dir=dirname(args.base))
    unified = join(tmpbase, 'all')

    exclude_packages_arg = []
    if args.exclude_packages is not None:
        exclude_packages_arg = ['--exclude-packages', args.exclude_packages]

    projects = sorted_deps()
    projects_arg = []
    if args.projects is not None:
        projects_arg = ['--projects', args.projects]
        projects = [project(name) for name in args.projects.split(',')]

    try:
        # Create javadoc for each project
        javadoc(['--base', tmpbase] + exclude_packages_arg + projects_arg)

        # Create unified javadoc for all projects
        title = args.title if args.title is not None else args.name
        javadoc(['--base', tmpbase,
                 '--unified',
                 '--arg', '@-windowtitle', '--arg', '@' + title,
                 '--arg', '@-doctitle', '--arg', '@' + title,
                 '--arg', '@-overview', '--arg', '@' + args.overview] + exclude_packages_arg + projects_arg)
        os.rename(join(tmpbase, 'javadoc'), unified)

        # Generate dependency graph with Graphviz
        if args.dot_output_base is not None:
            dot = join(tmpbase, 'all', str(args.dot_output_base) + '.dot')
            svg = join(tmpbase, 'all', str(args.dot_output_base) + '.svg')
            jpg = join(tmpbase, 'all', str(args.dot_output_base) + '.jpg')
            with open(dot, 'w') as fp:
                dim = len(projects)
                print >> fp, 'digraph projects {'
                print >> fp, 'rankdir=BT;'
                print >> fp, 'size = "' + str(dim) + ',' + str(dim) + '";'
                print >> fp, 'node [shape=rect, fontcolor="blue"];'
                #print >> fp, 'edge [color="green"];'
                for p in projects:
                    print >> fp, '"' + p.name + '" [URL = "../' + p.name + '/javadoc/index.html", target = "_top"]'
                    for dep in p.canonical_deps():
                        if dep in [proj.name for proj in projects]:
                            print >> fp, '"' + p.name + '" -> "' + dep + '"'
                depths = dict()
                for p in projects:
                    d = p.max_depth()
                    depths.setdefault(d, list()).append(p.name)
                print >> fp, '}'

            run(['dot', '-Tsvg', '-o' + svg, '-Tjpg', '-o' + jpg, dot])

        # Post-process generated SVG to remove title elements which most browsers
        # render as redundant (and annoying) tooltips.
        with open(svg, 'r') as fp:
            content = fp.read()
        content = re.sub('<title>.*</title>', '', content)
        content = re.sub('xlink:title="[^"]*"', '', content)
        with open(svg, 'w') as fp:
            fp.write(content)

        # Post-process generated overview-summary.html files

        def fix_overview_summary(path, topLink):
            """
            Processes an "overview-summary.html" generated by javadoc to put the complete
            summary text above the Packages table.
            """

            # This uses scraping and so will break if the relevant content produced by javadoc changes in any way!
            with open(path) as fp:
                content = fp.read()

            class Chunk:
                def __init__(self, content, ldelim, rdelim):
                    lindex = content.find(ldelim)
                    rindex = content.find(rdelim)
                    self.ldelim = ldelim
                    self.rdelim = rdelim
                    if lindex != -1 and rindex != -1 and rindex > lindex:
                        self.text = content[lindex + len(ldelim):rindex]
                    else:
                        self.text = None

                def replace(self, content, repl):
                    lindex = content.find(self.ldelim)
                    rindex = content.find(self.rdelim)
                    old = content[lindex:rindex + len(self.rdelim)]
                    return content.replace(old, repl)

            chunk1 = Chunk(content, """<div class="header">
<div class="subTitle">
<div class="block">""", """</div>
</div>
<p>See: <a href="#overview_description">Description</a></p>
</div>""")

            chunk2 = Chunk(content, """<div class="footer"><a name="overview_description">
<!--   -->
</a>
<div class="subTitle">
<div class="block">""", """</div>
</div>
</div>
<!-- ======= START OF BOTTOM NAVBAR ====== -->""")

            assert chunk1.text, 'Could not find header section in ' + path
            assert chunk2.text, 'Could not find footer section in ' + path

            content = chunk1.replace(content, '<div class="header"><div class="subTitle"><div class="block">' + topLink + chunk2.text +'</div></div></div>')
            content = chunk2.replace(content, '')

            with open(path, 'w') as fp:
                fp.write(content)

        top = join(tmpbase, 'all', 'overview-summary.html')
        for root, _, files in os.walk(tmpbase):
            for f in files:
                if f == 'overview-summary.html':
                    path = join(root, f)
                    topLink = ''
                    if top != path:
                        link = os.path.relpath(join(tmpbase, 'all', 'index.html'), dirname(path))
                        topLink = '<p><a href="' + link + '", target="_top"><b>[return to the overall ' + args.name + ' documentation]</b></a></p>'
                    fix_overview_summary(path, topLink)


        if exists(args.base):
            shutil.rmtree(args.base)
        shutil.move(tmpbase, args.base)

        print 'Created website - root is ' + join(args.base, 'all', 'index.html')

    finally:
        if exists(tmpbase):
            shutil.rmtree(tmpbase)

def findclass(args):
    """find all classes matching a given substring"""

    for entry, filename in classpath_walk(includeBootClasspath=True):
        if filename.endswith('.class'):
            if isinstance(entry, zipfile.ZipFile):
                classname = filename.replace('/', '.')
            else:
                classname = filename.replace(os.sep, '.')
            classname = classname[:-len('.class')]
            for a in args:
                if a in classname:
                    log(classname)

def javap(args):
    """launch javap with a -classpath option denoting all available classes

    Run the JDK javap class file disassembler with the following prepended options:

        -private -verbose -classpath <path to project classes>"""

    javap = java().javap
    if not exists(javap):
        abort('The javap executable does not exists: ' + javap)
    else:
        run([javap, '-private', '-verbose', '-classpath', classpath()] + args)

def show_projects(args):
    """show all loaded projects"""
    for s in suites():
        projectsFile = join(s.dir, 'mx', 'projects')
        if exists(projectsFile):
            log(projectsFile)
            for p in s.projects:
                log('\t' + p.name)

def add_argument(*args, **kwargs):
    """
    Define how a single command-line argument.
    """
    assert _argParser is not None
    _argParser.add_argument(*args, **kwargs)

# Table of commands in alphabetical order.
# Keys are command names, value are lists: [<function>, <usage msg>, <format args to doc string of function>...]
# If any of the format args are instances of Callable, then they are called with an 'env' are before being
# used in the call to str.format().
# Extensions should update this table directly
commands = {
    'about': [about, ''],
    'build': [build, '[options]'],
    'checkstyle': [checkstyle, ''],
    'canonicalizeprojects': [canonicalizeprojects, ''],
    'clean': [clean, ''],
    'eclipseinit': [eclipseinit, ''],
    'findclass': [findclass, ''],
    'help': [help_, '[command]'],
    'ideclean': [ideclean, ''],
    'ideinit': [ideinit, ''],
    'projectgraph': [projectgraph, ''],
    'javap': [javap, ''],
    'javadoc': [javadoc, '[options]'],
    'site': [site, '[options]'],
    'netbeansinit': [netbeansinit, ''],
    'projects': [show_projects, ''],
}

_argParser = ArgParser()

def _findPrimarySuite():
    # try current working directory first
    mxDir = join(os.getcwd(), 'mx')
    if exists(mxDir) and isdir(mxDir):
        return dirname(mxDir)

    # now search path of my executable
    me = sys.argv[0]
    parent = dirname(me)
    while parent:
        mxDir = join(parent, 'mx')
        if exists(mxDir) and isdir(mxDir):
            return parent
        parent = dirname(parent)
    return None

def main():
    primarySuiteDir = _findPrimarySuite()
    if primarySuiteDir:
        global _mainSuite
        _mainSuite = _loadSuite(primarySuiteDir, True)

    opts, commandAndArgs = _argParser._parse_cmd_line()

    global _opts, _java
    _opts = opts
    _java = JavaConfig(opts)

    for s in suites():
        s._post_init(opts)

    if len(commandAndArgs) == 0:
        _argParser.print_help()
        return

    command = commandAndArgs[0]
    command_args = commandAndArgs[1:]

    if not commands.has_key(command):
        abort('mx: unknown command \'{0}\'\n{1}use "mx help" for more options'.format(command, _format_commands()))

    c, _ = commands[command][:2]
    def term_handler(signum, frame):
        abort(1)
    signal.signal(signal.SIGTERM, term_handler)
    try:
        if opts.timeout != 0:
            def alarm_handler(signum, frame):
                abort('Command timed out after ' + str(opts.timeout) + ' seconds: ' + ' '.join(commandAndArgs))
            signal.signal(signal.SIGALRM, alarm_handler)
            signal.alarm(opts.timeout)
        retcode = c(command_args)
        if retcode is not None and retcode != 0:
            abort(retcode)
    except KeyboardInterrupt:
        # no need to show the stack trace when the user presses CTRL-C
        abort(1)

if __name__ == '__main__':
    # rename this module as 'mx' so it is not imported twice by the commands.py modules
    sys.modules['mx'] = sys.modules.pop('__main__')

    main()
