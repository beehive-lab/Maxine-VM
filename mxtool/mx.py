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
# mx is a command line tool inspired by mvn (http://maven.apache.org/)
# and hg (http://mercurial.selenic.com/). It includes a mechanism for
# managing the dependencies between a set of projects (like Maven)
# as well as making it simple to run commands
# (like hg is the interface to the Mercurial commands).
#
# The organizing principle of mx is a project suite. A suite is a directory
# containing one or more projects. It's not coincidental that this closely
# matches the layout of one or more projects in a Mercurial repository.
# The configuration information for a suite lives in an 'mx' sub-directory
# at the top level of the suite. 
#
# When launched, mx treats the current working directory as a suite.
# This is the primary suite. All other suites are called included suites.
#
# The configuration files (i.e. in the 'mx' sub-directory) of a suite are:
#
#   projects    - Defines the projects and libraries in the suite and the dependencies between them
#   commands.py - Suite specific extensions to the commands available to mx. This is only processed
#                 for the primary suite.
#   includes    - Other suites to be loaded. This is recursive. 
#   env         - A set of environment variable definitions.
#
# The includes and env files are typically not put under version control
# as they usually contain local file-system paths.
#
# The projects file is like the pom.xml file from Maven except that
# it is a properties file (not XML). Each non-comment line
# in the file specifies an attribute of a project or library. The main 
# difference between a project and a library is that the former contains
# source code built by the mx tool where as the latter is an external
# dependency. The format of the projects file is 
#
# Library specification format:
#
#     library@<name>@<prop>=<value>
#
# Built-in library properties (* = required):
#
#    *path: the file system path for the library to appear on a class path
#     urls: a comma seperated list of URLs from which the library can be downloaded
#     optional: if "true" then this library will be omitted from a class path if it doesn't exist on the file system and no URLs are specified
#
# Project specification format:
#
#     project@<name>@<prop>=<value>
#
# The name of a project also denotes the directory it is in.
#
# Built-in project properties:
#
#    *sourceDirs: a comma separated list of source directoriy names (relative to the project directory)
#     dependencies: a comma separated list of the libraries and project the project depends upon (transitive dependencies may be omitted)
#     checkstyle: the project whose Checkstyle configuration (i.e. <project>/.checkstyle_checks.xml) is used
#
# Other properties can be specified for projects and libraries for use by extension commands.
#
# Values can use environment variables with Bash syntax (e.g. ${HOME}).

import sys, os, errno, time, subprocess, shlex, types, urllib2, contextlib, StringIO, zipfile, signal
import shutil, fnmatch, re, xml.dom.minidom
from collections import Callable
from threading import Thread
from argparse import ArgumentParser, REMAINDER
from os.path import join, dirname, exists, getmtime, isabs, expandvars, isdir, isfile

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
    def __init__(self, suite, name, srcDirs, deps, dir):
        Dependency.__init__(self, suite, name)
        self.srcDirs = srcDirs
        self.deps = deps
        self.checkstyleProj = name
        self.native = False
        self.dir = dir
        
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

    def append_to_classpath(self, cp, resolve):
        if not self.native:
            cp.append(self.output_dir())

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
        if primary:
            self._load_commands(mxDir)

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
            subDir = attrs.pop('subDir', None);
            if subDir is None:
                dir = join(self.dir, name)
            else:
                dir = join(self.dir, subDir, name)
            p = Project(self, name, srcDirs, deps, dir)
            p.checkstyleProj = attrs.pop('checkstyle', name)
            p.native = attrs.pop('native', '') == 'true'
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
                    self.includes.append(expandvars_in_property(line.strip()))
        
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
        self._load_includes(mxDir)
        self._load_projects(mxDir)
        if self.mx_post_parse_cmd_line is not None:
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

def classpath(names=None, resolve=True, includeSelf=True):
    """
    Get the class path for a list of given projects, resolving each entry in the
    path (e.g. downloading a missing library) if 'resolve' is true.
    """
    if names is None:
        return _as_classpath(sorted_deps(True), resolve)
    deps = []
    if isinstance(names, types.StringTypes):
        project(names).all_deps(deps, True, includeSelf)
    else:
        for n in names:
            project(n).all_deps(deps, True, includeSelf)
    return _as_classpath(deps, resolve)
    
def sorted_deps(includeLibs=False):
    """
    Gets the loaded projects and libraries sorted such that dependencies
    are before the projects that depend on them. Unless 'includeLibs' is
    true, libraries are omitted from the result.
    """
    deps = []
    for p in _projects.itervalues():
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
            retcode = p.wait()
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
                retcode = p.wait()
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
            raise subprocess.CalledProcessError(retcode, ' '.join(args))
        abort(retcode)
        
    return retcode

def exe_suffix(name):
    """
    Gets the platform specific suffix for an executable 
    """
    if get_os() == 'windows':
        return name + '.exe'
    return name

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
        
        if self.debug_port is not None:
            self.java_args += ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=' + str(self.debug_port)]

    def format_cmd(self, args):
        return [self.java] + self.java_args_pfx + self.java_args + self.java_args_sfx + args
    
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
    
    parser = parser if parser is not None else ArgumentParser(prog='mx build')
    parser.add_argument('-f', action='store_true', dest='force', help='force compilation even if class files are up to date')
    parser.add_argument('-c', action='store_true', dest='clean', help='removes existing build output')
    parser.add_argument('--source', dest='compliance', help='Java compliance level', default='1.6')
    parser.add_argument('--Wapi', action='store_true', dest='warnAPI', help='show warnings about using internal APIs')
    parser.add_argument('--no-java', action='store_false', dest='java', help='do not build Java projects')
    parser.add_argument('--no-native', action='store_false', dest='native', help='do not build native projects')
    parser.add_argument('--jdt', help='Eclipse installation or path to ecj.jar for using the Eclipse batch compiler instead of javac', metavar='<path>')
    
    if suppliedParser:
        parser.add_argument('remainder', nargs=REMAINDER, metavar='...')

    args = parser.parse_args(args)
    
    jdtJar = None
    if args.jdt is not None:
        if args.jdt.endswith('.jar'):
            jdtJar=args.jdt
        elif isdir(args.jdt):
            plugins = join(args.jdt, 'plugins')
            choices = [f for f in os.listdir(plugins) if fnmatch.fnmatch(f, 'org.eclipse.jdt.core_*.jar')]
            if len(choices) != 0:
                jdtJar = join(plugins, sorted(choices, reverse=True)[0])

    built = set()
    for p in sorted_deps():
        
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
            
        javafilelist = []
        for sourceDir in sourceDirs:
            for root, _, files in os.walk(sourceDir):
                javafiles = [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
                javafilelist += javafiles
                
                # Copy all non Java resources
                nonjavafilelist = [join(root, name) for name in files if not name.endswith('.java')]
                for src in nonjavafilelist:
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
                errFilt = None
                if not args.warnAPI:
                    class Filter:
                        """
                        Class to errFilt the 'is Sun proprietary API and may be removed in a future release'
                        warning when compiling the VM classes.
                        
                        """
                        def __init__(self):
                            self.c = 0
                        
                        def eat(self, line):
                            if 'proprietary API' in line:
                                self.c = 2
                            elif self.c != 0:
                                self.c -= 1
                            else:
                                log(line.rstrip())
                    errFilt=Filter().eat
                    
                run([java().javac, '-g', '-J-Xmx1g', '-source', args.compliance, '-classpath', cp, '-d', outputDir, '@' + argfile.name], err=errFilt)
            else:
                log('Compiling Java sources for {0} with JDT...'.format(p.name))
                jdtProperties = join(p.dir, '.settings', 'org.eclipse.jdt.core.prefs')
                if not exists(jdtProperties):
                    raise SystemError('JDT properties file {0} not found'.format(jdtProperties))
                run([java().java, '-Xmx1g', '-jar', jdtJar,
                         '-properties', jdtProperties,
                         '-' + args.compliance,
                         '-cp', cp, '-g',
                         '-warn:-unusedImport,-unchecked',
                         '-d', outputDir, '@' + argfile.name])
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

            timestampFile = join(p.suite.dir, 'mx', '.checkstyle' + sourceDir[len(p.suite.dir):].replace(os.sep, '_') + '.timestamp')
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

def eclipseinit(args, suite=None):
    """(re)generate Eclipse project configurations"""

    if suite is None:
        suite = _mainSuite
        
    def println(out, obj):
        out.write(str(obj) + '\n')
        
    for p in projects():
        if p.native:
            continue
        
        if not exists(p.dir):
            os.makedirs(p.dir)

        out = StringIO.StringIO()
        
        println(out, '<?xml version="1.0" encoding="UTF-8"?>')
        println(out, '<classpath>')
        for src in p.srcDirs:
            srcDir = join(p.dir, src)
            if not exists(srcDir):
                os.mkdir(srcDir)
            println(out, '\t<classpathentry kind="src" path="' + src + '"/>')
    
        # Every Java program depends on the JRE
        println(out, '\t<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>')
        
        for dep in p.all_deps([], True):
            if dep == p:
                continue;
            
            if dep.isLibrary():
                if hasattr(dep, 'eclipse.container'):
                    println(out, '\t<classpathentry exported="true" kind="con" path="' + getattr(dep, 'eclipse.container') + '"/>')
                elif hasattr(dep, 'eclipse.project'):
                    println(out, '\t<classpathentry combineaccessrules="false" exported="true" kind="src" path="/' + getattr(dep, 'eclipse.project') + '"/>')
                else:
                    path = dep.path
                    if dep.mustExist:
                        dep.get_path(resolve=True)
                        if isabs(path):
                            println(out, '\t<classpathentry exported="true" kind="lib" path="' + path + '"/>')
                        else:
                            projRelPath = os.path.relpath(join(suite.dir, path), p.dir)
                            println(out, '\t<classpathentry exported="true" kind="lib" path="' + projRelPath + '"/>')
            else:
                println(out, '\t<classpathentry combineaccessrules="false" exported="true" kind="src" path="/' + dep.name + '"/>')
                        
        println(out, '\t<classpathentry kind="output" path="' + getattr(p, 'eclipse.output', 'bin') + '"/>')
        println(out, '</classpath>')
        update_file(join(p.dir, '.classpath'), out.getvalue())
        out.close()

        csConfig = join(project(p.checkstyleProj).dir, '.checkstyle_checks.xml')
        if exists(csConfig):
            out = StringIO.StringIO()
            
            dotCheckstyle = join(p.dir, ".checkstyle")
            checkstyleConfigPath = '/' + p.checkstyleProj + '/.checkstyle_checks.xml'
            println(out, '<?xml version="1.0" encoding="UTF-8"?>')
            println(out, '<fileset-config file-format-version="1.2.0" simple-config="true">')
            println(out, '\t<local-check-config name="Checks" location="' + checkstyleConfigPath + '" type="project" description="">')
            println(out, '\t\t<additional-data name="protect-config-file" value="false"/>')
            println(out, '\t</local-check-config>')
            println(out, '\t<fileset name="all" enabled="true" check-config-name="Checks" local="true">')
            println(out, '\t\t<file-match-pattern match-pattern="." include-pattern="true"/>')
            println(out, '\t</fileset>')
            println(out, '\t<filter name="FileTypesFilter" enabled="true">')
            println(out, '\t\t<filter-data value="java"/>')
            println(out, '\t</filter>')

            exclude = join(p.dir, '.checkstyle.exclude')
            if exists(exclude):
                println(out, '\t<filter name="FilesFromPackage" enabled="true">')
                with open(exclude) as f:
                    for line in f:
                        if not line.startswith('#'):
                            line = line.strip()
                            exclDir = join(p.dir, line)
                            assert isdir(exclDir), 'excluded source directory listed in ' + exclude + ' does not exist or is not a directory: ' + exclDir
                        println(out, '\t\t<filter-data value="' + line + '"/>')
                println(out, '\t</filter>')
                        
            println(out, '</fileset-config>')
            update_file(dotCheckstyle, out.getvalue())
            out.close()
        

        out = StringIO.StringIO()
        
        println(out, '<?xml version="1.0" encoding="UTF-8"?>')
        println(out, '<projectDescription>')
        println(out, '\t<name>' + p.name + '</name>')
        println(out, '\t<comment></comment>')
        println(out, '\t<projects>')
        println(out, '\t</projects>')
        println(out, '\t<buildSpec>')
        println(out, '\t\t<buildCommand>')
        println(out, '\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>')
        println(out, '\t\t\t<arguments>')
        println(out, '\t\t\t</arguments>')
        println(out, '\t\t</buildCommand>')
        if exists(csConfig):
            println(out, '\t\t<buildCommand>')
            println(out, '\t\t\t<name>net.sf.eclipsecs.core.CheckstyleBuilder</name>')
            println(out, '\t\t\t<arguments>')
            println(out, '\t\t\t</arguments>')
            println(out, '\t\t</buildCommand>')
        println(out, '\t</buildSpec>')
        println(out, '\t<natures>')
        println(out, '\t\t<nature>org.eclipse.jdt.core.javanature</nature>')
        if exists(csConfig):
            println(out, '\t\t<nature>net.sf.eclipsecs.core.CheckstyleNature</nature>')
        println(out, '\t</natures>')
        println(out, '</projectDescription>')
        update_file(join(p.dir, '.project'), out.getvalue())
        out.close()

        out = StringIO.StringIO()
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
                    update_file(join(settingsDir, name), content)

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

        out = StringIO.StringIO()
        
        println(out, '<?xml version="1.0" encoding="UTF-8"?>')
        println(out, '<project name="' + p.name + '" default="default" basedir=".">')
        println(out, '\t<description>Builds, tests, and runs the project ' + p.name + '.</description>')
        println(out, '\t<import file="nbproject/build-impl.xml"/>')
        println(out, '</project>')
        updated = update_file(join(p.dir, 'build.xml'), out.getvalue()) or updated
        out.close()
        
        out = StringIO.StringIO()
        println(out, '<?xml version="1.0" encoding="UTF-8"?>')
        println(out, '<project xmlns="http://www.netbeans.org/ns/project/1">')
        println(out, '    <type>org.netbeans.modules.java.j2seproject</type>')
        println(out, '    <configuration>')
        println(out, '        <data xmlns="http://www.netbeans.org/ns/j2se-project/3">')
        println(out, '            <name>' + p.name+ '</name>')
        println(out, '            <explicit-platform explicit-source-supported="true"/>')
        println(out, '            <source-roots>')
        println(out, '                <root id="src.dir"/>')
        println(out, '            </source-roots>')
        println(out, '            <test-roots>')
        println(out, '                <root id="test.src.dir"/>')
        println(out, '            </test-roots>')
        println(out, '        </data>')
        
        firstDep = True
        for dep in p.all_deps([], True):
            if dep == p:
                continue;
            
            if not dep.isLibrary():
                n = dep.name.replace('.', '_')
                if firstDep:
                    println(out, '        <references xmlns="http://www.netbeans.org/ns/ant-project-references/1">')
                    firstDep = False
                    
                println(out, '            <reference>')
                println(out, '                <foreign-project>' + n + '</foreign-project>')
                println(out, '                <artifact-type>jar</artifact-type>')
                println(out, '                <script>build.xml</script>')
                println(out, '                <target>jar</target>')
                println(out, '                <clean-target>clean</clean-target>')
                println(out, '                <id>jar</id>')
                println(out, '            </reference>')
            
        if not firstDep:
            println(out, '        </references>')
            
        println(out, '    </configuration>')
        println(out, '</project>')
        updated = update_file(join(p.dir, 'nbproject', 'project.xml'), out.getvalue()) or updated
        out.close()
        
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
        println(out, content)

        mainSrc = True
        for src in p.srcDirs:
            srcDir = join(p.dir, src)
            if not exists(srcDir):
                os.mkdir(srcDir)
            ref = 'file.reference.' + p.name + '-' + src
            println(out, ref + '=' + src)
            if mainSrc:
                println(out, 'src.dir=${' + ref + '}')
                mainSrc = False
            else:
                println(out, 'src.' + src + '.dir=${' + ref + '}')
            
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
                println(out, ref + '=' + path)
                    
            else:
                n = dep.name.replace('.', '_')
                relDepPath = os.path.relpath(dep.dir, p.dir).replace(os.sep, '/')
                ref = 'reference.' + n + '.jar'
                println(out, 'project.' + n + '=' + relDepPath)
                println(out, ref + '=${project.' + n + '}/dist/' + dep.name + '.jar')
                
            javacClasspath.append('${' + ref + '}')
            
        println(out, 'javac.classpath=\\\n    ' + (os.pathsep + '\\\n    ').join(javacClasspath))
        

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
    'build': [build, '[options]'],
    'checkstyle': [checkstyle, ''],
    'canonicalizeprojects': [canonicalizeprojects, ''],
    'clean': [clean, ''],
    'eclipseinit': [eclipseinit, ''],
    'help': [help_, '[command]'],
    'ideclean': [ideclean, ''],
    'ideinit': [ideinit, ''],
    'projectgraph': [projectgraph, ''],
    'javap': [javap, ''],
    'netbeansinit': [netbeansinit, ''],
    'projects': [show_projects, ''],
}

_argParser = ArgParser()

def main():
    cwdMxDir = join(os.getcwd(), 'mx')
    if exists(cwdMxDir) and isdir(cwdMxDir):
        global _mainSuite
        _mainSuite = _loadSuite(os.getcwd(), True)
            
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
