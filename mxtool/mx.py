#!/usr/bin/python
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
# When launched, mx looks for a mx configuration (i.e. a directory named 'mx') 
# in the current working directory. This is the primary mx configuration. Any
# other mx configurations are included mx configurations.
#
# If an mx configuration exists, then the following files in the configuration
# are processed (if they exist):
#
#   projects    - Lists projects, libraries and dependencies between them
#   commands.py - Extensions to the commands launchable by mx. This is only processed
#                 for the primary mx configuration.
#   includes    - Other directories containing mx configurations to be loaded.
#                 This is a recursive action. 
#   env         - A set of environment variable definitions.
#
# The MX_INCLUDES environment variable can also be used to specify
# other directories containing mx configurations.
# This value of this variable has the same format as a Java class path.
#
# The includes and env files are typically not put under version control
# as they usually contain local filesystem paths.
#
# The projects file is like the pom.xml file from Maven except that
# it is in a properties file format instead of XML. Each non-comment line
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

import sys
import os
import subprocess
from collections import Callable
from threading import Thread
from argparse import ArgumentParser, REMAINDER
from os.path import join, dirname, exists, getmtime, isabs, expandvars, isdir
import shlex
import types
import urllib2
import contextlib
import StringIO
import zipfile
import shutil, fnmatch, re, xml.dom.minidom

DEFAULT_JAVA_ARGS = '-ea -Xss2m -Xmx1g'

class Dependency:
    def __init__(self, name, baseDir):
        self.name = name
        self.baseDir = baseDir
        self.env = None
        
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
    def __init__(self, baseDir, name, srcDirs, deps):
        Dependency.__init__(self, name, baseDir)
        self.srcDirs = srcDirs
        self.deps = deps
        self.checkstyleProj = name
        self.dir = join(baseDir, name)
        self.native = False
        
    def all_deps(self, deps, pdb, includeLibs):
        if self in deps:
            return deps
        for name in self.deps:
            assert name != self.name
            dep = pdb.libs.get(name, None)
            if dep is not None:
                if includeLibs and not dep in deps:
                    deps.append(dep)
            else:
                dep = pdb.project(name)
                if not dep in deps:
                    dep.all_deps(deps, pdb, includeLibs)
        if not self in deps:
            deps.append(self)
        return deps
    
    def _compute_max_dep_distances(self, name, distances, dist, pdb):
        currentDist = distances.get(name);
        if currentDist is None or currentDist < dist:
            distances[name] = dist
            if pdb.projects.has_key(name):
                p = pdb.project(name)
                for dep in p.deps:
                    self._compute_max_dep_distances(dep, distances, dist + 1, pdb)
                

    def canonical_deps(self, env, pdb):
        distances = dict()
        result = set()
        self._compute_max_dep_distances(self.name, distances, 0, pdb)
        for n,d in distances.iteritems():
            assert d > 0 or n == self.name
            if d == 1:
                result.add(n)
                
            
        if len(result) == len(self.deps) and frozenset(self.deps) == result:
            return self.deps
        return result;
    

    def source_dirs(self):
        return [join(self.baseDir, self.name, s) for s in self.srcDirs]
        
    def output_dir(self):
        return join(self.baseDir, self.name, 'bin')

    def classpath(self, resolve, env):
        classesDir = join(self.baseDir, 'classes')
        if exists(classesDir):
            return [self.output_dir(), classesDir]
        return [self.output_dir()]
    


class Library(Dependency):
    def __init__(self, baseDir, name, path, mustExist, urls):
        Dependency.__init__(self, name, baseDir)
        self.path = path
        self.urls = urls
        self.mustExist = mustExist
    
    def classpath(self, resolve, env):
        path = self.path
        if not isabs(path):
            path = join(self.baseDir, path)
        if resolve and self.mustExist and not exists(path):
            assert not len(self.urls) == 0, 'cannot find required library  ' + self.name + " " + path;
            env.download(path, self.urls)

        if exists(path) or not resolve:
            return [path]
        return []
    
class ProjectsDB():
    
    def __init__(self, env):
        self.env = env
        self.projects = dict()
        self.libs = dict()
        self.commandModules = dict()
        self.baseDirs = []
        self.primary = ''

    def _load_projects(self, mxDir, baseDir):
        env = self.env
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
                    if len(parts) != 3:
                        env.abort('Property name does not have 3 parts separated by "@": ' + key)
                    kind, name, attr = parts
                    if kind == 'project':
                        m = projsMap
                    elif kind == 'library':
                        m = libsMap
                    else:
                        env.abort('Property name does not start with "project@" or "library@": ' + key)
                        
                    attrs = m.get(name)
                    if attrs is None:
                        attrs = dict()
                        m[name] = attrs
                    value = env.expandvars_in_property(value)
                    attrs[attr] = value
                        
        def pop_list(attrs, name):
            v = attrs.pop(name, None)
            if v is None or len(v.strip()) == 0:
                return []
            return [n.strip() for n in v.split(',')]
        
        for name, attrs in projsMap.iteritems():
            if self.projects.has_key(name):
                env.abort('cannot override project  ' + name + ' in ' + self.project(name).baseDir + " with project of the same name in  " + mxDir)
            srcDirs = pop_list(attrs, 'sourceDirs')
            deps = pop_list(attrs, 'dependencies')
            subDir = attrs.pop('subDir', '');
            p = Project(join(baseDir, subDir), name, srcDirs, deps)
            p.checkstyleProj = attrs.pop('checkstyle', name)
            p.native = attrs.pop('native', '') == 'true'
            p.__dict__.update(attrs)
            self.projects[name] = p

        for name, attrs in libsMap.iteritems():
            if self.libs.has_key(name):
                env.abort('cannot redefine library ' + name)
            
            path = attrs['path']
            mustExist = attrs.pop('optional', 'false') != 'true'
            urls = pop_list(attrs, 'urls')
            l = Library(baseDir, name, path, mustExist, urls)
            l.__dict__.update(attrs)
            self.libs[name] = l
        
    def _load_commands(self, mxDir, baseDir):
        env = self.env
        commands = join(mxDir, 'commands.py')
        if exists(commands):
            # temporarily extend the Python path
            sys.path.insert(0, mxDir)
    
            mod = __import__('commands')
    
            # revert the Python path
            del sys.path[0]

            if not hasattr(mod, 'mx_init'):
                env.abort(commands + ' must define an mx_init(env) function')
                
            mod.mx_init(env)
                
            name = baseDir + '.commands'
            sfx = 1
            while sys.modules.has_key(name):
                name = baseDir + str(sfx) + '.commands'
                sfx += 1
            
            sys.modules[name] = sys.modules.pop('commands')
            self.commandModules[name] = mod
                
    def _load_includes(self, mxDir, baseDir):
        includes = join(mxDir, 'includes')
        if exists(includes):
            with open(includes) as f:
                for line in f:
                    includeMxDir = join(self.env.expandvars_in_property(line.strip()), 'mx')
                    self.load(includeMxDir)
        
    def _load_env(self, mxDir, baseDir):
        env = join(mxDir, 'env')
        if exists(env):
            with open(env) as f:
                for line in f:
                    line = line.strip()
                    if len(line) != 0 and line[0] != '#':
                        key, value = line.split('=', 1)
                        os.environ[key.strip()] = self.env.expandvars_in_property(value.strip())
        
    def load(self, mxDir, primary=False):
        """ loads the mx data from a given directory """
        if not exists(mxDir) or not isdir(mxDir):
            self.env.abort('Directory does not exist: ' + mxDir)
        baseDir = dirname(mxDir)
        if primary:
            self.primary = baseDir
        if not baseDir in self.baseDirs:
            self.baseDirs.append(baseDir)
            self._load_includes(mxDir, baseDir)
            self._load_projects(mxDir, baseDir)
            self._load_env(mxDir, baseDir)
            if primary:
                self._load_commands(mxDir, baseDir)

    def project_names(self):
        return ' '.join(self.projects.keys())
        
    def project(self, name, fatalIfMissing=True):
        p = self.projects.get(name)
        if p is None:
            self.env.abort('project named ' + name + ' not found')
        return p
    
    def library(self, name):
        l = self.libs.get(name)
        if l is None:
            self.env.abort('library named ' + name + ' not found')
        return l

    def _as_classpath(self, deps, resolve):
        cp = []
        if self.env.cp_prefix is not None:
            cp = [self.env.cp_prefix]
        for d in deps:
            cp += d.classpath(resolve, self.env)
        if self.env.cp_suffix is not None:
            cp += [self.env.cp_suffix]
        return os.pathsep.join(cp)

    def classpath(self, names=None, resolve=True):
        if names is None:
            return self._as_classpath(self.sorted_deps(True), resolve)
        deps = []
        if isinstance(names, types.StringTypes):
            self.project(names).all_deps(deps, self, True)
        else:
            for n in names:
                self.project(n).all_deps(deps, self, True)
        return self._as_classpath(deps, resolve)
        
    def sorted_deps(self, includeLibs=False):
        deps = []
        for p in self.projects.itervalues():
            p.all_deps(deps, self, includeLibs)
        return deps

class Env(ArgumentParser):

    def format_commands(self):
        msg = '\navailable commands:\n\n'
        for cmd in sorted(self.commands.iterkeys()):
            c, _ = self.commands[cmd][:2]
            doc = c.__doc__
            if doc is None:
                doc = ''
            msg += ' {0:<20} {1}\n'.format(cmd, doc.split('\n', 1)[0])
        return msg + '\n'
    
    # Override parent to append the list of available commands
    def format_help(self):
        return ArgumentParser.format_help(self) + self.format_commands()
    
    
    def __init__(self):
        self.java_initialized = False
        self.pdb = ProjectsDB(self)
        self.commands = dict()
        ArgumentParser.__init__(self, prog='mx')
    
        self.add_argument('-v', action='store_true', dest='verbose', help='enable verbose output')
        self.add_argument('-d', action='store_true', dest='java_dbg', help='make Java processes wait on port 8000 for a debugger')
        self.add_argument('--cp-pfx', dest='cp_prefix', help='class path prefix', metavar='<arg>')
        self.add_argument('--cp-sfx', dest='cp_suffix', help='class path suffix', metavar='<arg>')
        self.add_argument('--J', dest='java_args', help='Java VM arguments (e.g. --J @-dsa)', metavar='@<args>', default=DEFAULT_JAVA_ARGS)
        self.add_argument('--Jp', action='append', dest='java_args_pfx', help='prefix Java VM arguments (e.g. --Jp @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--Ja', action='append', dest='java_args_sfx', help='suffix Java VM arguments (e.g. --Ja @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--user-home', help='users home directory', metavar='<path>', default=os.path.expanduser('~'))
        self.add_argument('--java-home', help='JDK installation directory (must be JDK 6 or later)', metavar='<path>', default=self.default_java_home())
        self.add_argument('--java', help='Java VM executable (default: bin/java under $JAVA_HOME)', metavar='<path>')
        self.add_argument('--os', dest='os', help='operating system override')
        
    def _parse_cmd_line(self, args=None):
        if args is None:
            args = sys.argv[1:]
        
        self.add_argument('commandAndArgs', nargs=REMAINDER, metavar='command args...')
        
        self.parse_args(namespace=self)

        if self.java_home is None or self.java_home == '':
            self.abort('Could not find Java home. Use --java-home option or ensure JAVA_HOME environment variable is set.')

        if self.user_home is None or self.user_home == '':
            self.abort('Could not find user home. Use --user-home option or ensure HOME environment variable is set.')

        if self.os is None:
            self.remote = False
            if sys.platform.startswith('darwin'):
                self.os = 'darwin'
            elif sys.platform.startswith('linux'):
                self.os = 'linux'
            elif sys.platform.startswith('sunos'):
                self.os = 'solaris'
            elif sys.platform.startswith('win32') or sys.platform.startswith('cygwin'):
                self.os = 'windows'
            else:
                print 'Supported operating system could not be derived from', sys.platform, '- use --os option explicitly.'
                sys.exit(1)
        else:
            self.java_args += ' -Dmax.os=' + self.os 
            self.remote = True 
    
        if self.java is None:
            self.java = join(self.java_home, 'bin', 'java')
    
        os.environ['JAVA_HOME'] = self.java_home
        os.environ['HOME'] = self.user_home
 
        self.javac = join(self.java_home, 'bin', 'javac')
        
        for mod in self.pdb.commandModules.itervalues():
            if hasattr(mod, 'mx_post_parse_cmd_line'):
                mod.mx_post_parse_cmd_line(self)

    def expandvars_in_property(self, value):
        result = expandvars(value)
        if '$' in result or '%' in result:
            self.abort('Property contains an undefined environment variable: ' + value)
        return result
        

    def load_config_file(self, configFile, override=False):
        """ adds attributes to this object from a file containing key=value lines """
        if exists(configFile):
            with open(configFile) as f:
                for line in f:
                    k, v = line.split('=', 1)
                    k = k.strip().lower()
                    if (override or not hasattr(self, k)):
                        setattr(self, k, self.expandvars_in_property(v.strip()))
                        
    def format_java_cmd(self, args):
        self.init_java()
        return [self.java] + self.java_args_pfx + self.java_args + self.java_args_sfx + args
        
    def run_java(self, args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
        return self.run(self.format_java_cmd(args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)
    
    def run(self, args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
        """
        Run a command in a subprocess, wait for it to complete and return the exit status of the process.
        If the exit status is non-zero and `nonZeroIsFatal` is true, then the program is exited with
        the same exit status.
        Each line of the standard output and error streams of the subprocess are redirected to the
        provided out and err functions if they are not None.
        """
        
        assert isinstance(args, types.ListType), "'args' must be a list: " + str(args)
        for arg in args:
            assert isinstance(arg, types.StringTypes), 'argument is not a string: ' + str(arg)
        
        if self.verbose:
            self.log(' '.join(args))
            
        try:
            if out is None and err is None:
                retcode = subprocess.call(args, cwd=cwd)
            else:
                def redirect(stream, f):
                    for line in iter(stream.readline, ''):
                        f(line)
                    stream.close()
                p = subprocess.Popen(args, cwd=cwd, stdout=None if out is None else subprocess.PIPE, stderr=None if err is None else subprocess.PIPE)
                if out is not None:
                    t = Thread(target=redirect, args=(p.stdout, out))
                    t.daemon = True # thread dies with the program
                    t.start()
                if err is not None:
                    t = Thread(target=redirect, args=(p.stderr, err))
                    t.daemon = True # thread dies with the program
                    t.start()
                retcode = p.wait()
        except OSError as e:
            self.log('Error executing \'' + ' '.join(args) + '\': ' + str(e))
            if self.verbose:
                raise e
            self.abort(e.errno)
        

        if retcode and nonZeroIsFatal:
            if self.verbose:
                raise subprocess.CalledProcessError(retcode, ' '.join(args))
            self.abort(retcode)
            
        return retcode

    def check_get_env(self, key):
        """
        Gets an environment variable, aborting with a useful message if it is not set.
        """
        value = os.environ.get(key)
        if value is None:
            self.abort('Required environment variable ' + key + ' must be set (e.g. in ' + join(self.pdb.primary, 'env') + ')')
        return value

    def exe_suffix(self, name):
        """
        Gets the platform specific suffix for an executable 
        """
        if self.os == 'windows':
            return name + '.exe'
        return name
    
    def log(self, msg=None):
        """
        Write a message to the console.
        All script output goes through this method thus allowing a subclass
        to redirect it. 
        """
        if msg is None:
            print
        else:
            print msg

    def expand_project_in_class_path_arg(self, cpArg):
        cp = []
        for part in cpArg.split(os.pathsep):
            if part.startswith('@'):
                cp += self.pdb.classpath(part[1:]).split(os.pathsep)
            else:
                cp.append(part)
        return os.pathsep.join(cp)
        
    def expand_project_in_args(self, args):
        for i in range(len(args)):
            if args[i] == '-cp' or args[i] == '-classpath':
                if i + 1 < len(args):
                    args[i + 1] = self.expand_project_in_class_path_arg(args[i + 1])
                return
    

    def init_java(self):
        """
        Lazy initialization and preprocessing of this object's fields before running a Java command.
        """
        if self.java_initialized:
            return

        def delAtAndSplit(s):
            return shlex.split(s.lstrip('@'))

        self.java_args = delAtAndSplit(self.java_args)
        self.java_args_pfx = sum(map(delAtAndSplit, self.java_args_pfx), [])
        self.java_args_sfx = sum(map(delAtAndSplit, self.java_args_sfx), [])
        
        # Prepend the -d64 VM option only if the java command supports it
        output = ''
        try:
            output = subprocess.check_output([self.java, '-d64', '-version'], stderr=subprocess.STDOUT)
            self.java_args = ['-d64'] + self.java_args
        except subprocess.CalledProcessError as e:
            try:
                output = subprocess.check_output([self.java, '-version'], stderr=subprocess.STDOUT)
            except subprocess.CalledProcessError as e:
                print e.output
                self.abort(e.returncode)

        output = output.split()
        assert output[0] == 'java' or output[0] == 'openjdk'
        assert output[1] == 'version'
        version = output[2]
        if not version.startswith('"1.6') and not version.startswith('"1.7'):
            self.abort('Requires Java version 1.6 or 1.7, got version ' + version)

        if self.java_dbg:
            self.java_args += ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000']
            
        self.java_initialized = True
    
    def default_java_home(self):
        javaHome = os.getenv('JAVA_HOME')
        if javaHome is None:
            if exists('/usr/lib/java/java-6-sun'):
                javaHome = '/usr/lib/java/java-6-sun'
            elif exists('/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home'):
                javaHome = '/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home'
            elif exists('/usr/jdk/latest'):
                javaHome = '/usr/jdk/latest'
        return javaHome

    def gmake_cmd(self):
        for a in ['make', 'gmake', 'gnumake']:
            try:
                output = subprocess.check_output([a, '--version'])
                if 'GNU' in output:
                    return a;
            except:
                pass
        self.abort('Could not find a GNU make executable on the current path.')

           
    def abort(self, codeOrMessage):
        """
        Aborts the program with a SystemExit exception.
        If 'codeOrMessage' is a plain integer, it specifies the system exit status;
        if it is None, the exit status is zero; if it has another type (such as a string),
        the object's value is printed and the exit status is one.
        """
        raise SystemExit(codeOrMessage)

    def download(self, path, urls):
        """
        Attempts to downloads content for each URL in a list, stopping after the first successful download.
        If the content cannot be retrieved from any URL, the program is aborted. The downloaded content
        is written to the file indicated by 'path'.
        """
        d = dirname(path)
        if d != '' and not exists(d):
            os.makedirs(d)
            
        def url_open(url):
            userAgent = 'Mozilla/5.0 (compatible)'
            headers = { 'User-Agent' : userAgent }
            req = urllib2.Request(url, headers=headers)
            return urllib2.urlopen(req);
            
        for url in urls:
            try:
                self.log('Downloading ' + url + ' to ' + path)
                if url.startswith('zip:') or url.startswith('jar:'):
                    i = url.find('!/')
                    if i == -1:
                        self.abort('Zip or jar URL does not contain "!/": ' + url)
                    url, _, entry = url[len('zip:'):].partition('!/')
                    with contextlib.closing(url_open(url)) as f:
                        data = f.read()
                        zipdata = StringIO.StringIO(f.read())
                
                    zf = zipfile.ZipFile(zipdata, 'r')
                    data = zf.read(entry)
                    with open(path, 'w') as f:
                        f.write(data)
                else:
                    with contextlib.closing(url_open(url)) as f:
                        data = f.read()
                    with open(path, 'w') as f:
                        f.write(data)
                return
            except IOError as e:
                self.log('Error reading from ' + url + ': ' + str(e))
            except zipfile.BadZipfile as e:
                self.log('Error in zip file downloaded from ' + url + ': ' + str(e))
                
        # now try it with Java - urllib2 does not handle meta refreshes which are used by Sourceforge
        myDir = dirname(__file__)
        
        javaSource = join(myDir, 'URLConnectionDownload.java')
        javaClass = join(myDir, 'URLConnectionDownload.class')
        if not exists(javaClass) or getmtime(javaClass) < getmtime(javaSource):
            subprocess.check_call([self.javac, '-d', myDir, javaSource])
        if self.run([self.java, '-cp', myDir, 'URLConnectionDownload', path] + urls) != 0:
            self.abort('Could not download to ' + path + ' from any of the following URLs:\n\n    ' +
                      '\n    '.join(urls) + '\n\nPlease use a web browser to do the download manually')

    def update_file(self, path, content):
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
                
            self.log(('modified ' if existed else 'created ') + path)
            return True;
        except IOError as e:
            self.abort('Error while writing to ' + path + ': ' + str(e));

# Builtin commands
            
def build(env, args):
    """compile the Java and C sources, linking the latter

    Compile all the Java source code using the appropriate compilers
    and linkers for the various source code types."""
    
    parser = ArgumentParser(prog='mx build');
    parser.add_argument('-f', action='store_true', dest='force', help='force compilation even if class files are up to date')
    parser.add_argument('-c', action='store_true', dest='clean', help='removes existing build output')
    parser.add_argument('--no-native', action='store_false', dest='native', help='do not build com.oracle.max.vm.native')
    parser.add_argument('--jdt', help='Eclipse installation or path to ecj.jar for using the Eclipse batch compiler instead of javac', metavar='<path>')

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

    projects = [p.name for p in env.pdb.sorted_deps()]
    built = set()
    for project in projects:
        p = env.pdb.project(project)
        projectDir = join(p.baseDir, project)
        
        if p.native:
            if env.os == 'windows':
                env.log('Skipping C compilation on Windows until it is supported')
                pass
            
            env.log('Compiling C sources in {0}...'.format(projectDir))

            if args.clean:
                env.run([env.gmake_cmd(), 'clean'], cwd=projectDir)
                
            env.run([env.gmake_cmd()], cwd=projectDir)
            built.add(project)
            continue
        
        outputDir = p.output_dir()
        if exists(outputDir):
            if args.clean:
                env.log('Cleaning {0}...'.format(outputDir))
                shutil.rmtree(outputDir)
                os.mkdir(outputDir)
        else:
            os.mkdir(outputDir)

        classpath = env.pdb.classpath(project)
        sourceDirs = env.pdb.project(project).source_dirs()
        mustBuild = args.force
        if not mustBuild:
            for dep in p.all_deps([], env.pdb, False):
                if dep.name in built:
                    mustBuild = True
            
        for sourceDir in sourceDirs:
            javafilelist = []
            nonjavafilelist = []
            for root, _, files in os.walk(sourceDir):
                javafiles = [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
                javafilelist += javafiles
                nonjavafilelist += [join(root, name) for name in files if not name.endswith('.java')]
                if not mustBuild:
                    for javafile in javafiles:
                        classfile = outputDir + javafile[len(sourceDir):-len('java')] + 'class'
                        if not exists(classfile) or os.path.getmtime(javafile) > os.path.getmtime(classfile):
                            mustBuild = True
                            break
                
            if not mustBuild:
                env.log('[all class files in {0} are up to date - skipping]'.format(sourceDir))
                continue
                
            if len(javafilelist) == 0:
                env.log('[no Java sources in {0} - skipping]'.format(sourceDir))
                continue

            built.add(project)

            argfileName = join(projectDir, 'javafilelist.txt')
            argfile = open(argfileName, 'w')
            argfile.write('\n'.join(javafilelist))
            argfile.close()
            
            try:
                if jdtJar is None:
                    env.log('Compiling Java sources in {0} with javac...'.format(sourceDir))
                    
                    class Filter:
                        """
                        Class to filter the 'is Sun proprietary API and may be removed in a future release'
                        warning when compiling the VM classes.
                        
                        """
                        def __init__(self):
                            self.c = 0
                        
                        def eat(self, line):
                            if 'Sun proprietary API' in line:
                                self.c = 2
                            elif self.c != 0:
                                self.c -= 1
                            else:
                                print line.rstrip()
                        
                    env.run([env.javac, '-g', '-J-Xmx1g', '-classpath', classpath, '-d', outputDir, '@' + argfile.name], err=Filter().eat)
                else:
                    env.log('Compiling Java sources in {0} with JDT...'.format(sourceDir))
                    jdtProperties = join(projectDir, '.settings', 'org.eclipse.jdt.core.prefs')
                    if not exists(jdtProperties):
                        raise SystemError('JDT properties file {0} not found'.format(jdtProperties))
                    env.run([env.java, '-Xmx1g', '-jar', jdtJar, '-1.6', '-cp', classpath, '-g',
                             '-properties', jdtProperties, 
                             '-warn:-unusedImport,-unchecked',
                             '-d', outputDir, '@' + argfile.name])
            finally:
                os.remove(argfileName)
                        
                
            for name in nonjavafilelist:
                dst = join(outputDir, name[len(sourceDir) + 1:])
                if exists(dirname(dst)):
                    shutil.copyfile(name, dst)

def canonicalizeprojects(env, args):
    """process all project files to canonicalize the dependencies

    The exit code of this command reflects how many files were updated."""
    
    changedFiles = 0
    pdb = env.pdb
    for d in pdb.baseDirs:
        projectsFile = join(d, 'mx', 'projects')
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
                    p = pdb.project(m.group(1))
                    out.write('project@' + m.group(1) + '@dependencies=' + ','.join(p.canonical_deps(env, pdb)) + '\n')
            content = out.getvalue()
        if env.update_file(projectsFile, content):
            changedFiles += 1
    return changedFiles;
    
def checkstyle(env, args):
    """run Checkstyle on the Java sources

   Run Checkstyle over the Java sources. Any errors or warnings
   produced by Checkstyle result in a non-zero exit code.

If no projects are given, then all Java projects are checked."""
    
    allProjects = [p.name for p in env.pdb.sorted_deps()]
    if len(args) == 0:
        projects = allProjects
    else:
        projects = args
        unknown = set(projects).difference(allProjects)
        if len(unknown) != 0:
            env.error('unknown projects: ' + ', '.join(unknown))
        
    for project in projects:
        p = env.pdb.project(project)
        projectDir = join(p.baseDir, project)
        sourceDirs = env.pdb.project(project).source_dirs()
        dotCheckstyle = join(projectDir, '.checkstyle')
        
        if not exists(dotCheckstyle):
            continue
        
        for sourceDir in sourceDirs:
            javafilelist = []
            for root, _, files in os.walk(sourceDir):
                javafilelist += [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
            if len(javafilelist) == 0:
                env.log('[no Java sources in {0} - skipping]'.format(sourceDir))
                continue

            timestampFile = join(p.baseDir, 'mx', '.checkstyle' + sourceDir[len(p.baseDir):].replace(os.sep, '_') + '.timestamp')
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
                env.log('[all Java sources in {0} already checked - skipping]'.format(sourceDir))
                continue

            if exists(timestampFile):                
                os.utime(timestampFile, None)
            else:
                file(timestampFile, 'a')
            
            dotCheckstyleXML = xml.dom.minidom.parse(dotCheckstyle)
            localCheckConfig = dotCheckstyleXML.getElementsByTagName('local-check-config')[0]
            configLocation = localCheckConfig.getAttribute('location')
            if configLocation.startswith('/'):
                config = join(p.baseDir, configLocation.lstrip('/'))
            else:
                config = join(projectDir, configLocation)
                
            exclude = join(projectDir, '.checkstyle.exclude')
                
                
            
            
            if exists(exclude):
                with open(exclude) as f:
                    # Convert patterns to OS separators
                    patterns = [name.rstrip().replace('/', os.sep) for name in f.readlines()]
                def match(name):
                    for p in patterns:
                        if p in name:
                            env.log('excluding: ' + name)
                            return True
                    return False
                    
                javafilelist = [name for name in javafilelist if not match(name)]
            
            auditfileName = join(projectDir, 'checkstyleOutput.txt')
            env.log('Running Checkstyle on {0} using {1}...'.format(sourceDir, config))
            
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
                        env.run_java(['-Xmx1g', '-jar', env.pdb.library('CHECKSTYLE').classpath(True, env)[0], '-c', config, '-o', auditfileName] + batch)
                    finally:
                        if exists(auditfileName):
                            with open(auditfileName) as f:
                                warnings = [line.strip() for line in f if 'warning:' in line]
                                if len(warnings) != 0:
                                    map(env.log, warnings)
                                    return 1
            finally:
                if exists(auditfileName):
                    os.unlink(auditfileName)
    return 0

def clean(env, args):
    """remove all class files, images, and executables

    Removes all files created by a build, including Java class files, executables, and
    generated images.
    """
    
    projects = env.pdb.projects.keys()
    for project in projects:
        p = env.pdb.project(project)
        if p.native:
            env.run([env.gmake_cmd(), '-C', p.dir, 'clean'])
        else:
            outputDir = p.output_dir()
            if outputDir != '' and exists(outputDir):
                env.log('Removing {0}...'.format(outputDir))
                shutil.rmtree(outputDir)
    
def help_(env, args):
    """show help for a given command

With no arguments, print a list of commands and short help for each command.

Given a command name, print help for that command."""
    if len(args) == 0:
        env.print_help()
        return
    
    name = args[0]
    if not env.commands.has_key(name):
        env.error('unknown command: ' + name)
    
    value = env.commands[name]
    (func, usage) = value[:2]
    doc = func.__doc__
    if len(value) > 2:
        docArgs = value[2:]
        fmtArgs = []
        for d in docArgs:
            if isinstance(d, Callable):
                fmtArgs += [d(env)]
            else:
                fmtArgs += [str(d)]
        doc = doc.format(*fmtArgs)
    print 'mx {0} {1}\n\n{2}\n'.format(name, usage, doc)


# Commands are in alphabetical order in this file.

def javap(env, args):
    """launch javap with a -classpath option denoting all available classes

    Run the JDK javap class file disassembler with the following prepended options:

        -private -verbose -classpath <path to project classes>"""
        
    javap = join(env.java_home, 'bin', 'javap')
    if not exists(javap):
        env.abort('The javap executable does not exists: ' + javap)
    else:
        env.run([javap, '-private', '-verbose', '-classpath', env.pdb.classpath()] + args)

def projects(env, args):
    """show all loaded projects"""
    pdb = env.pdb
    for d in pdb.baseDirs:
        projectsFile = join(d, 'mx', 'projects')
        if exists(projectsFile):
            env.log('# file:  ' + projectsFile)
            for p in pdb.projects.values():
                if p.baseDir == d:
                    env.log(p.name)


def main(env):    

    # Table of commands in alphabetical order.
    # Keys are command names, value are lists: [<function>, <usage msg>, <format args to doc string of function>...]
    # If any of the format args are instances of Callable, then they are called with an 'env' are before being
    # used in the call to str.format().  
    # Extensions should update this table directly
    env.commands = {
        'build': [build, '[options] projects...'],
        'checkstyle': [checkstyle, 'projects...'],
        'canonicalizeprojects': [canonicalizeprojects, ''],
        'clean': [clean, ''],
        'help': [help_, '[command]'],
        'javap': [javap, ''],
        'projects': [projects, ''],
    }
    
    MX_INCLUDES = os.environ.get('MX_INCLUDES', None)
    if MX_INCLUDES is not None:
        for path in MX_INCLUDES.split(os.pathsep):
            d = join(path, 'mx')
            if exists(d) and isdir(d):
                env.pdb.load(d)
                
    cwdMxDir = join(os.getcwd(), 'mx')
    if exists(cwdMxDir) and isdir(cwdMxDir):
        env.pdb.load(cwdMxDir, primary=True)
            
    env._parse_cmd_line()
    
    if len(env.commandAndArgs) == 0:
        env.print_help()
        return
    
    env.command = env.commandAndArgs[0]
    env.command_args = env.commandAndArgs[1:]
    
    if not env.commands.has_key(env.command):
        env.abort('mx: unknown command \'{0}\'\n{1}use "mx help" for more options'.format(env.command, env.format_commands()))
        
    c, _ = env.commands[env.command][:2]
    try:
        retcode = c(env, env.command_args)
        if retcode is not None and retcode != 0:
            env.abort(retcode)
    except KeyboardInterrupt:
        # no need to show the stack trace when the user presses CTRL-C
        env.abort(1)

if __name__ == '__main__':
    main(Env())
