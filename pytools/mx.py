#!/usr/bin/python
#
# mx.py - shell interface for Maxine source code
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
# A launcher for Maxine executables and tools. This launch simplifies the task
# of running the Maxine VM and utilities by setting up the necessary class paths
# and executable paths. The only requirement is for the user to have set the
# environment variable JAVA_HOME to point to a JDK installation directory.
# The '-java_home' global option can be used instead.
#
# The commands are defined in commands.py.
#
# The mymx.py file gives an example of how to extend this launcher.
#

import sys
import os
import subprocess
from threading import Thread
from argparse import ArgumentParser, REMAINDER
from os.path import join, dirname, abspath, exists, getmtime
import commands
import shlex
import types

DEFAULT_JAVA_ARGS = '-ea -Xss2m -Xmx1g'

class Env(ArgumentParser):

    # Override parent to append the list of available commands
    def format_help(self):
        msg = ArgumentParser.format_help(self) + '\navailable commands:\n\n'
        for cmd in commands.table.iterkeys():
            c, _ = commands.table[cmd]
            doc = c.__doc__
            msg += ' {0:<16} {1}\n'.format(cmd, doc.split('\n', 1)[0])
        return msg + '\n'
    
    def __init__(self):
        self.java_initialized = False
        ArgumentParser.__init__(self, prog='max')
    
        self.add_argument('-v', action='store_true', dest='verbose', help='enable verbose output')
        self.add_argument('-d', action='store_true', dest='java_dbg', help='make Java processes wait on port 8000 for a debugger')
        self.add_argument('--cp-pfx', dest='cp_prefix', help='class path prefix', metavar='<arg>')
        self.add_argument('--cp-sfx', dest='cp_suffix', help='class path suffix', metavar='<arg>')
        self.add_argument('--J', dest='java_args', help='Java VM arguments (e.g. --J @-dsa)', metavar='@<args>', default=DEFAULT_JAVA_ARGS)
        self.add_argument('--Jp', action='append', dest='java_args_pfx', help='prefix Java VM arguments (e.g. --Jp @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--Ja', action='append', dest='java_args_sfx', help='suffix Java VM arguments (e.g. --Ja @-dsa)', metavar='@<args>', default=[])
        self.add_argument('--user-home', help='users home directory', metavar='<path>', default=os.path.expanduser('~'))
        self.add_argument('--java-home', help='JDK installation directory (must be JDK 6 or later)', metavar='<path>', default=default_java_home())
        self.add_argument('--java', help='Java VM executable (default: bin/java under JAVA_HOME)', metavar='<path>')
        self.add_argument('--os', dest='os', help='operating system hosting the VM (all lower case) for remote inspecting')
        self.add_argument('-V', '--vmdir', dest='vmdir',
                          metavar='<path>', 
                          help='directory for VM executable, shared libraries boot image and related files')
        self.add_argument('-M', '--maxine', dest='maxine_dir',
                          metavar='<path>', 
                          help='base directory of the Maxine code base')
        
        self.add_argument('commandAndArgs', nargs=REMAINDER, metavar='command args...')
        
        self.parse_args(namespace=self)

        if self.java_home is None or self.java_home == '':
            self.log('Could not find Java home. Use --java-home option or ensure JAVA_HOME environment variable is set.')
            abort(1)

        if self.user_home is None or self.user_home == '':
            self.log('Could not find user home. Use --user-home option or ensure HOME environment variable is set.')
            abort(1)

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
 
        if self.maxine_dir is None:
            self.maxine_dir = dirname(abspath(dirname(sys.argv[0])))
    
        if self.vmdir is None:
            self.vmdir = join(self.maxine_dir, 'com.oracle.max.vm.native', 'generated', self.os)
            
        self.maxvm_options = os.getenv('MAXVM_OPTIONS', '')
        self.javac = join(self.java_home, 'bin', 'javac')

    def jmax(self, args):
        """ executes a jmax.java command, returning stdout as a string """
        os.environ['maxine_dir'] = self.maxine_dir
        if self.cp_prefix is not None:
            os.environ['cp_prefix'] = self.cp_prefix
        if self.cp_suffix is not None:
            os.environ['cp_suffix'] = self.cp_suffix
        
        jmaxDir = join(self.maxine_dir, 'com.oracle.max.shell')
        jmaxClass = join(jmaxDir, 'jmax.class')
        jmaxSource = join(jmaxDir, 'jmax.java')
        if  not exists(jmaxClass) or getmtime(jmaxClass) < getmtime(jmaxSource):
            subprocess.check_call([self.javac, '-d', dirname(jmaxClass), jmaxSource])
        
        return subprocess.check_output([self.java, '-ea', '-cp', jmaxDir, 'jmax'] + args).strip()
        
    def classpath(self, project):
        return self.jmax(['classpath', project])
            
    def run_java(self, args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
        self._init_java()
        return self.run([self.java] + self.java_args_pfx + self.java_args + self.java_args_sfx + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)
    
    def run(self, args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
        """
        
        Run a command in a subprocess, wait for it to complete and return the exit status of the process.
        If the exit status is non-zero and `nonZeroIsFatal` is true, then the program is exited with
        the same exit status.
        Each line of the standard output and error streams of the subprocess are redirected to the
        provided out and err functions if they are not None.
        
        """
        
        assert isinstance(args, types.ListType), "'args' must be a list of strings: " + str(args)
        
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
                p = subprocess.Popen(args, stdout=None if out is None else subprocess.PIPE, stderr=None if err is None else subprocess.PIPE)
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
            abort(e.errno)
        

        if retcode and nonZeroIsFatal:
            if self.verbose:
                raise subprocess.CalledProcessError(retcode, ' '.join(args))
            abort(retcode)
            
        return retcode

    
    def log(self, msg):
        print msg

    def _init_java(self):
        if self.java_initialized:
            return

        def delAt(s):
            return s.lstrip('@')
        
        self.java_args = shlex.split(delAt(self.java_args))
        self.java_args_pfx = map(shlex.split, map(delAt, self.java_args_pfx))
        self.java_args_sfx = map(shlex.split, map(delAt, self.java_args_sfx))
        
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
                abort(e.returncode)

        output = output.split()
        assert output[0] == 'java'
        assert output[1] == 'version'
        version = output[2]
        if not version.startswith('"1.6') and not version.startswith('"1.7'):
            self.log('Requires Java version 1.6 or 1.7, got version ' + version)
            abort(1)

        if self.java_dbg:
            self.java_args += ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000']
            
        self.java_initialized = True
    
def default_java_home():
    javaHome = os.getenv('JAVA_HOME')
    if javaHome is None:
        if exists('/usr/lib/java/java-6-sun'):
            javaHome = '/usr/lib/java/java-6-sun'
        elif exists('/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home'):
            javaHome = '/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home'
        elif exists('/usr/jdk/latest'):
            javaHome = '/usr/jdk/latest'
    return javaHome
       
def abort(code):
    """ raises a SystemExit exception with the provided exit code """
    raise SystemExit(code)
    
def main():
    env = Env()

    if len(env.commandAndArgs) == 0:
        env.print_help()
        return
    
    env.command = env.commandAndArgs[0]
    env.command_args = env.commandAndArgs[1:]
    
    if not commands.table.has_key(env.command):
        env.error('unknown command "' + env.command + '"')
        
    c, _ = commands.table[env.command]
    retcode = c(env, env.command_args)
    if retcode is not None and retcode != 0:
        abort(retcode)
    
    
#This idiom means the below code only runs when executed from command line
if __name__ == '__main__':
    main()
    
