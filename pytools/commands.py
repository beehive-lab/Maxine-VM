#
# commands.py - the commands available to mx.py
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

import mx
import os
import shutil
import fnmatch
import tempfile
from os.path import join, exists, dirname
from argparse import ArgumentParser, REMAINDER
from subprocess import CalledProcessError

def jnigen(env, args):
    """(re)generate content in JniFunctions.java from JniFunctionsSource.java

    Run JniFunctionsGenerator.java to update the methods in JniFunctions.java by adding
    a prologue and epilogue to the @JNI_FUNCTION annotated methods in JniFunctionsSource.java.

    The exit code is non-zero if JniFunctions.java was modified."""

    if len(args) != 0:
        env.print_help()
        mx.abort(1)

    return env.run_java(['-cp', env.classpath('com.oracle.max.vm'), 'com.sun.max.vm.jni.JniFunctionsGenerator'])

def help(env, args):
    """show help for a given command

With no arguments, print a list of commands and short help for each command.

Given a command name, print help for that command."""
    if len(args) == 0:
        env.print_help()
        return
    
    name = args[0]
    if not table.has_key(name):
        env.error('unknown command: ' + name)
    
    (func, usage) = table[name]
    doc = func.__doc__
    print 'max {0} {1}\n\n{2}\n'.format(name, usage, doc)
    
def build(env, args):
    """compile the Maxine Java and C sources, linking the latter

    Compile all or some of the Maxine source code using the appropriate compilers
    and linkers for the various source code types.

    If no projects are given, then all projects are built."""
    
    parser = ArgumentParser(prog='max build');
    parser.add_argument('-c', action='store_true', dest='clean', help='removes existing build output')
    parser.add_argument('projects', nargs=REMAINDER, metavar='projects...')
    parser.add_argument('--jdt', help='Eclipse installation or path to ecj.jar for using the Eclipse batch compiler instead of javac', metavar='<path>')

    args = parser.parse_args(args)
    
    jdtJar = None
    if args.jdt is not None:
        if args.jdt.endswith('.jar'):
            jdtJar=args.jdt
        elif os.path.isdir(args.jdt):
            plugins = join(args.jdt, 'plugins')
            choices = [f for f in os.listdir(plugins) if fnmatch.fnmatch(f, 'org.eclipse.jdt.core_*.jar')]
            if len(choices) != 0:
                jdtJar = join(plugins, sorted(choices, reverse=True)[0])

    allProjects = ['com.oracle.max.vm.native'] + env.jmax(['projects']).split()
    if len(args.projects) == 0:
        projects = allProjects
    else:
        projects = args.projects
        unknown = set(projects).difference(allProjects)
        if len(unknown) != 0:
            parser.error('unknown projects: ' + ', '.join(unknown))
        
    for project in projects:
        projectDir = join(env.maxine_dir, project)
        
        if project == 'com.oracle.max.vm.native':
            env.log('Compiling C sources in {0}...'.format(projectDir))

            if args.clean:
                env.run(['gmake', '-C', projectDir, 'clean'])
                
            env.run(['gmake', '-C', projectDir])
            continue
        
        outputDir = env.jmax(['output_dir', project])
        if exists(outputDir):
            if args.clean:
                env.log('Cleaning {0}...'.format(outputDir))
                shutil.rmtree(outputDir)
                os.mkdir(outputDir)
        else:
            os.mkdir(outputDir)
            
        classpath = env.jmax(['classpath', project])
        sourceDirs = env.jmax(['source_dirs', project]).split()
        for sourceDir in sourceDirs:
            javafilelist = []
            nonjavafilelist = []
            for root, _, files in os.walk(sourceDir):
                javafilelist += [join(root, name) for name in files if name.endswith('.java') and name != 'package-info.java']
                nonjavafilelist += [join(root, name) for name in files if not name.endswith('.java')]
            if len(javafilelist) == 0:
                env.log('[no Java sources in {0} - skipping]'.format(sourceDir))
                continue
            
            (_, tmpfile) = tempfile.mkstemp(prefix='javafiles-', suffix='.list')
            argfile = open(tmpfile, 'w')
            argfile.write('\n'.join(javafilelist))
            argfile.close()
            
            try:
                if jdtJar is None:
                    env.log('Compiling Java sources in {0} with javac...'.format(sourceDir))
                    
                    class Filter:
                        """
                        Class to filter the 'sun.misc.Signal is Sun proprietary API and may be removed in a future release'
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
                        
                    f = Filter()
                    env.run_redirect([env.javac, '-g', '-J-Xmx1g', '-classpath', classpath, '-d', outputDir, '@' + argfile.name], err=f.eat, nonZeroIsError=True)
                else:
                    env.log('Compiling Java sources in {0} with JDT...'.format(sourceDir))
                    jdtProperties = join(projectDir, '.settings', 'org.eclipse.jdt.core.prefs')
                    if not exists(jdtProperties):
                        raise SystemError('JDT properties file {0} not found'.format(jdtProperties))
                    env.run([env.java, '-Xmx1g', '-jar', jdtJar, '-1.7', '-cp', classpath,
                             '-properties', jdtProperties, 
                             '-warn:-unusedImport,-unchecked',
                             '-d', outputDir, '@' + argfile.name], nonZeroIsError=True)
            finally:
                os.unlink(argfile.name)
                
            for name in nonjavafilelist:
                dst = join(outputDir, name[len(sourceDir) + 1:])
                if exists(dirname(dst)):
                    shutil.copyfile(name, dst)

def clean(env, args):
    """remove all class files, images, and executables

    Removes all files created by a build, including Java class files, executables, and
    generated images.
    """
    
    env.run(['gmake', '-C', join(env.maxine_dir, 'com.oracle.max.vm.native'), 'clean'])

    projects = env.jmax(['projects']).split()
    for project in projects:
        outputDir = env.jmax(['output_dir', project])
        if outputDir != '':
            env.log('Removing {0}...'.format(outputDir))
            shutil.rmtree(outputDir)

table = {
    'jnigen': (jnigen, ''),
    'build': (build, 'projects...'),
    'help': (help, '[command]'),
    'clean': (clean, '')
}