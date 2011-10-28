#
# commands.py - the default commands available to mx.py
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
import subprocess
from os.path import join, exists, dirname, isdir, pathsep
from argparse import ArgumentParser, REMAINDER
from collections import Callable
import xml.dom.minidom

# Commands are in alphabetical order in this file.

def build(env, args):
    """compile the Maxine Java and C sources, linking the latter

    Compile all or some of the Maxine source code using the appropriate compilers
    and linkers for the various source code types.

    If no projects are given, then all projects are built."""
    
    parser = ArgumentParser(prog='mx build');
    parser.add_argument('-c', action='store_true', dest='clean', help='removes existing build output')
    parser.add_argument('--no-native', action='store_false', dest='native', help='do not build com.oracle.max.vm.native')
    parser.add_argument('projects', nargs=REMAINDER, metavar='projects...')
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

    allProjects = env.jmax(['projects']).split()
    if args.native:
        if env.os == 'windows':
            env.log('Skipping C compilation on Windows until it is supported')
            pass
        else:
            allProjects = ['com.oracle.max.vm.native'] + allProjects
    
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
                env.run(['gmake', 'clean'], cwd=projectDir)
                
            env.run(['gmake'], cwd=projectDir)
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

            argfileName = join(projectDir, 'javafilelist.txt')
            argfile = open(argfileName, 'w')
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

def c1x(env, args):
    """alias for "mx olc -c=C1X ..." """
    olc(env, ['-c=C1X'] + args)

def canonicalizeprojects(env, args):
    """process all project.properties files to canonicalize the project dependencies

    The exit code of this command reflects how many files were updated."""
    env.run_jmax(['canonicalizeprojects'])
    
def check(env, args):
    """run Checkstyle on the Maxine Java sources

   Run Checkstyle over the Java sources. Any errors or warnings
   produced by Checkstyle result in a non-zero exit code.

If no projects are given, then all Java projects are checked."""
    
    allProjects = env.jmax(['projects']).split()
    if len(args) == 0:
        projects = allProjects
    else:
        projects = args
        unknown = set(projects).difference(allProjects)
        if len(unknown) != 0:
            env.error('unknown projects: ' + ', '.join(unknown))
        
    for project in projects:
        projectDir = join(env.maxine_dir, project)
        sourceDirs = env.jmax(['source_dirs', project]).split()
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

            dotCheckstyleXML = xml.dom.minidom.parse(dotCheckstyle)
            localCheckConfig = dotCheckstyleXML.getElementsByTagName('local-check-config')[0]
            configLocation = localCheckConfig.getAttribute('location')
            if configLocation.startswith('/'):
                config = join(env.maxine_dir, configLocation.lstrip('/'))
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
                        env.run_java(['-Xmx1g', '-jar', env.jmax(['library', 'CHECKSTYLE']), '-c', config, '-o', auditfileName] + batch)
                    finally:
                        with open(auditfileName) as f:
                            warnings = [line.strip() for line in f if 'warning:' in line]
                            if len(warnings) != 0:
                                map(env.log, warnings)
                                mx.abort(1)
            finally:
                os.unlink(auditfileName)

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

def _configs(env):
    class Configs:
        def __init__(self):
            self.configs = dict()
            
        def eat(self, line):
            (k, v) = line.split('#')
            self.configs[k] = v.rstrip()
    c = Configs()        
    env.run([env.java, '-client', '-Xmx40m', '-Xms40m', '-XX:NewSize=30m', '-cp', env.classpath('com.oracle.max.vm'), 'test.com.sun.max.vm.MaxineTesterConfiguration'], out=c.eat)
    return c.configs

def configs(env, arg):
    """prints the predefined image configurations"""
    c = _configs(env)
    env.log('The available preconfigured option sets are:')
    env.log()
    env.log('    Configuration    Expansion')
    for k, v in sorted(c.iteritems()):
        env.log('    @{0:<16} {1}'.format(k, v.replace(',', ' ')))
    
def copycheck(env, args):
    """run copyright check on the Maxine sources (defined as being under hg control)"""
    env.run_java(['-cp', env.jmax(['classpath_noresolve', 'com.oracle.max.base']), 'com.sun.max.tools.CheckCopyright'] + args)

def eclipseprojects(env, args):
    """(re)generate Eclipse project configurations

    The exit code of this command reflects how many files were updated."""
    env.run_jmax(['eclipse'] + args)

def gate(env, args):
    """run the tests used to validate a push to the stable Maxine repository

    If this commands exits with a 0 exit code, then the source code is in
    a state that would be accepted for integration into the main repository."""
    
    env.log('Running copycheck')
    hgNode = os.getenv('hg_node')
    if hgNode is None:
        copycheck(env, ['-modified', '-reporterrors=true', '-continueonerror'])
    else:
        revTip = subprocess.check_output(['hg', 'tip', '--template', "'{rev}'"])
        revLast = subprocess.check_output(['hg', 'log', '-r', hgNode, '--template', "'{rev}'"])
        changesetCount=revTip - revLast + 1
        copycheck(env, ['-last=' + changesetCount, '-reporterrors=true', '-continueonerror'])

    env.log('Ensuring JavaTester harness is up to date')
    try:
        jttgen(env, [])
    except SystemExit:
        env.log('Updated JavaTesterRunScheme.java or JavaTesterTests.java in com.sun.max.vm.jtrun.all.')
        env.log('To push your changes to the repository, these files need to be generated locally and checked in.')
        env.log('The files can be generated by running: mx jttgen')
        mx.abort(1)

    env.log('Ensuring Eclipse project files match auto-generated content')
    try:
        eclipseprojects(env, [])
    except SystemExit:
        env.log('One or more Eclipse project files differ from the content generated')
        env.log('by running: mx eclipseprojects')
        mx.abort(1)

    env.log('Ensuring projects.properties files are canonicalized')
    try:
        canonicalizeprojects(env, [])
    except SystemExit:
        env.log('Rerun "mx canonicalizeprojects" and check-in the modified projects.properties files.')
        mx.abort(1)

    env.log('Running MaxineTester...')

    test(env, ['-image-configs=java', '-fail-fast'] + args)
    
def graal(env, args):
    """alias for "mx olc -c=Graal ..." """
    olc(env, ['-c=Graal'] + args)

def hcfdis(env, args):
    """disassembles HexCodeFiles embedded in text files

    Run a tool over the input files to convert all embedded HexCodeFiles
    to a disassembled format."""
    env.run_java(['-cp', env.classpath('com.oracle.max.hcfdis'), 'com.oracle.max.hcfdis.HexCodeFileDis'] + args)

def helloworld(env, args):
    """run the 'hello world' program on the Maxine VM"""
    env.run([join(env.vmdir, 'maxvm'), '-cp', env.classpath('com.oracle.max.vm')] + args + ['test.output.HelloWorld'])

def help_(env, args):
    """show help for a given command

With no arguments, print a list of commands and short help for each command.

Given a command name, print help for that command."""
    if len(args) == 0:
        env.print_help()
        return
    
    name = args[0]
    if not table.has_key(name):
        env.error('unknown command: ' + name)
    
    value = table[name]
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

def image(env, args):
    """build a boot image

    Run the BootImageGenerator to build a Maxine boot image. The classes
    and packages specified on the command line will be included in the
    boot image in addition to those found by the Package.java mechanism.
    Package names are differentiated from class names by being prefixed
    with '^'.

    The platform configuration for the generated image is auto-detected
    by native methods. However, the following system properties can be
    used to override the defaults:

    Name            | Description                   | Example values
    ================+===============================+================
    max.platform    | name of a preset platform     | solaris-amd64 linux-amd64 darwin-amd64
    max.cpu         | processor model               | AMD64 IA32 SPARCV9
    max.isa         | instruction set architecture  | AMD64 ARM PPC SPARC
    max.os          | operating system              | Darwin Linux Solaris
    max.endianness  | endianness                    | BIG LITTLE
    max.bits        | machine word size             | 64 32
    max.page        | page size                     | 4096 8192
    max.nsig        | number of signals             | 32

    These system properties can be specified as options to the image
    command (e.g. '-os Darwin -bits 32').

    An option starting with '@' denotes one of the preconfigured set of
    options described by running "mx options".
    
    An option starting with '--' is interpreted as a VM option of the same name
    after the leading '-' is removed. For example, to use the '-verbose:class'
    VM option to trace class loading while image building, specify '--verbose:class'.
    Note that not all VM options have an effect during image building.

    Use "mx image -help" to see what other options this command accepts."""

    systemProps = []
    imageArgs = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg[0] == '@':
            name = arg.lstrip('@')
            configs = _configs(env)
            if not name in configs:
                env.log('Invalid image configuration: ' + name)
                help(env, ['image'])
                mx.abort()
            values = configs[name].split(',')
            del args[i]
            args[i:i] = values
            continue
        elif arg in ['-platform', '-cpu', '-isa', '-os', '-endianness', '-bits', '-page', '-nsig']:
            name = arg.lstrip('-')
            i += 1
            if i == len(args):
                env.log('Missing value for ' + arg)
                help(env, ['image'])
                mx.abort()
            value = args[i]
            systemProps += ['-Dmax.' + name + '=' + value]
        elif arg.startswith('--XX:LogFile='):
            os.environ['MAXINE_LOG_FILE'] = arg.split('=', 1)[1]
        elif arg == '-vma':
            systemProps += ['-Dmax.permsize=2', '-Dmax.vmthread.factory.class=com.oracle.max.vm.ext.vma.runtime.VMAVmThreadFactory']
        else:
            imageArgs += [arg]
        i += 1

    env.run_java(systemProps + ['-cp', env.classpath(), 'com.sun.max.vm.hosted.BootImageGenerator', '-trace=' + str(env.java_trace), '-run=java'] + imageArgs)

def inspect(env, args):
    """launch a given program under the Inspector

    Run Maxine under the Inspector. The arguments accepted by this command
    are those accepted by the 'mx vm' command plus the Inspector specific
    options. To debug a program in the Inspector, simply replace 'vm' on the
    command line that launches the program with 'inspect'.

    Use "mx inspect --help" to see what the Inspector options are. These options
    must be specified with a '--' prefix so that they can be distinguished from
    the VM options.

    The inspect command also accepts the same system property related options
    as the 'image' command except that a '--' prefix must be used (e.g.
    '--os Darwin --bits 32'). Use "mx help image" for more detail.

    Use "mx vm -help" to see what the VM options are."""

    saveClassDir = join(env.vmdir, 'inspected_classes')
    maxvmOptions = os.getenv('MAXVM_OPTIONS', '').split()
    vmArgs = ['-XX:SaveClassDir=' + saveClassDir, '-XX:+TrapOnError'] + maxvmOptions
    insArgs = ['-vmdir=' + env.vmdir]
    if not isdir(saveClassDir):
        os.makedirs(saveClassDir)
    sysProps = []
    insCP = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg.startswith('-XX:LogFile='):
            logFile = arg.split('=', 1)[1]
            vmArgs += [arg]
            os.environ['TELE_LOG_FILE'] = 'tele-' + logFile
        elif arg in ['-cp', '-classpath']:
            vmArgs += [arg, args[i + 1]]
            insCP += [args[i + 1]]
            i += 1
        elif arg == '-jar':
            vmArgs += ['-jar', args[i + 1]]
            insCP += [args[i + 1]]
            i += 1
        elif arg in ['--platform', '--cpu', '--isa', '--os', '--endianness', '--bits', '--page', '--nsig']:
            name = arg.lstrip('-')
            i += 1
            value = args[i]
            sysProps += ['-Dmax.' + name + '=' + value]
        elif arg.startswith('--cp='):
            insCP += [arg[len('--cp='):]]
        elif arg.startswith('--'):
            # chomp leading '-'
            insArgs += [arg[1:]]
        elif arg.startswith('-XX:SaveClassDir='):
            vmArgs += [arg]
            saveClassDir = arg.split('=', 1)[1]
            if not isdir(saveClassDir):
                os.makedirs(saveClassDir)
        elif arg.startswith('-'):
            vmArgs += [arg]
        else:
            # This is the main class argument; copy it and any following
            # arguments to the VM verbatim
            vmArgs += args[i:]
            break
        i += 1
    
    insCP += [saveClassDir]
    insCP = pathsep.join(insCP)
    insArgs += ['-cp=' + insCP]
    
    cmd = env.format_java_cmd(sysProps + ['-cp', env.classpath() + pathsep + insCP, 'com.sun.max.ins.MaxineInspector'] +
                              insArgs + ['-a=' + ' '.join(vmArgs)])
    
    if not env.remote and env.os == 'darwin':
        # The -E option propagates the environment variables into the sudo process
        env.run(['sudo', '-E', '-p', 'Debugging is a privileged operation on Mac OS X.\nPlease enter your "sudo" password:'] + cmd)
    else:
        env.run(cmd)

def jnigen(env, args):
    """(re)generate content in JniFunctions.java from JniFunctionsSource.java

    Run JniFunctionsGenerator.java to update the methods in JniFunctions.java by adding
    a prologue and epilogue to the @JNI_FUNCTION annotated methods in JniFunctionsSource.java.

    The exit code is non-zero if JniFunctions.java was modified."""

    return env.run_java(['-cp', env.classpath('com.oracle.max.vm'), 'com.sun.max.vm.jni.JniFunctionsGenerator'])

def jttgen(env, args):
    """(re)generate harness and run scheme for the JavaTester tests

    Run the JavaTester to update the JavaTesterRunScheme.java and JavaTesterTests.java
    files in the com.sun.max.vm.jtrun.all package."""

    testDir = join(env.maxine_dir, 'com.oracle.max.vm', 'test')
    tests = [join('jtt', name) for name in os.listdir(join(testDir, 'jtt')) if name != 'hotspot' and name != 'fail']
    return env.run_java(['-cp', env.classpath('com.oracle.max.vm'), 'test.com.sun.max.vm.compiler.JavaTester',
                         '-scenario=target', '-run-scheme-package=all', '-native-tests'] + tests, cwd=testDir)

def methodtree(env, args):
    """print the causality spanning-tree of the method graph in the boot image

    The causality spanning-tree allows one to audit the boot image with respect
    to why any given method is (or isn't) in the image. This is useful when
    trying to reduce the size of the image.

    This tool requires an input *.tree file which is produced by specifying the
    -tree option when building the boot image.

    Use "mx methodtree -help" to see what other options this command accepts."""

    env.run_java(['-cp', env.classpath(), 'com.sun.max.vm.hosted.BootImageMethodTree', '-in=' + join(env.vmdir, 'maxine.method.tree')] + args)

def nm(env, args):
    """print the contents of a boot image

    Print the contents of a boot image in a textual form.
    If not specified, the following path will be used for the boot image file:

        {0}

    Use "mx nm -help" to see what other options this command accepts."""

    env.run_java(['-cp', env.classpath(), 'com.sun.max.vm.hosted.BootImagePrinter'] + args + [join(env.vmdir, 'maxine.vm')])

def objecttree(env, args):
    """print the causality spanning-tree of the object graph in the boot image

    The causality spanning-tree allows one to audit the boot image with respect
    to why any given object is in the image. This is useful when trying to reduce
    the size of the image.

    This tool requires an input *.tree file which is produced by specifying the
    -tree option when building the boot image.

    Use "mx objecttree -help" to see what other options this command accepts."""

    env.run_java(['-cp', env.classpath(), 'com.sun.max.vm.hosted.BootImageObjectTree', '-in=' + join(env.vmdir, 'maxine.object.tree')] + args)


def olc(env, args):
    """offline compile a list of methods

    See Patterns below for a description of the format expected for "patterns..."

    The output traced by this command is not guaranteed to be the same as the output
    for a compilation performed at runtime. The code produced by a compiler is sensitive
    to the compilation context such as what classes have been resolved etc.

    Use "mx olc -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    env.run_java(['-cp', env.classpath(), 'com.oracle.max.vm.ext.maxri.Compile'] + args)

def projects(env, args):
    """show all project properties derived from the projects.properties files"""
    env.log(env.run_jmax(['properties'] + args))

def t1x(env, args):
    """alias for "mx olc -c=T1X ..." """
    olc(env, ['-c=T1X'] + args)

def t1xgen(env, args):
    """(re)generate content in T1XTemplateSource.java

    Run T1XTemplateGenerator.java to generate the auto-generated templates in T1XTemplateSource.java.

    The exit code is non-zero if the auto-generated part of T1XTemplateSource.java was modified."""

    return env.run_java(['-cp', env.classpath('com.oracle.max.vm.ext.t1x'), 'com.oracle.max.vm.ext.t1x.T1XTemplateGenerator'])

def test(env, args):
    """run some or all of the Maxine tests

    The Maxine sources include a variety of tests that can be run by a
    special launcher. These include JUnit tests, VM micro tests, certain
    benchmark suites and output comparison tests, amongst others.

    Use "mx test -help" to see what other options this command accepts."""
    maxineTesterDir = join(env.maxine_dir, 'maxine-tester')
    if isdir(maxineTesterDir):
        for root, _, files in os.walk(maxineTesterDir):
            for name in files:
                if name.rsplit(', ', 1) in ['stdout', 'stderr', 'passed', 'failed', 'command']:
                    os.remove(join(root, name))
    else:
        os.mkdir(maxineTesterDir)
    
    class Tee:
        def __init__(self, f):
            self.f = f
        def eat(self, line):
            env.log(line.rstrip())
            self.f.write(line)
    
    env.init_java()
    console = join(maxineTesterDir, 'console')
    with open(console, 'w', 0) as f:
        tee = Tee(f)
        env.run_java(['-cp', env.classpath(), 'test.com.sun.max.vm.MaxineTester', '-output-dir=maxine-tester',
                      '-refvm=' + env.java, '-refvm-args=' + ' '.join(env.java_args)] + args, out=tee.eat, err=tee.eat)

def verify(env, args):
    """verifies a set of methods using the Maxine bytecode verifier

    Run the Maxine verifier over a set of specified methods available
    on the class path. To extend the class path, use one of the global
    "--cp-pfx" or "--cp-sfx" options.

    See Patterns below for a description of the format expected for "patterns..."

    Use "mx verify -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    env.run_java(['-cp', env.classpath(), 'test.com.sun.max.vm.verifier.CommandLineVerifier'] + args)
            
def vm(env, args):
    """launch the Maxine VM

    Run the Maxine VM with the given options and arguments.
    The expansion of the MAXVM_OPTIONS environment variable is inserted
    before any other VM options specified on the command line.

    Use "mx vm -help" to see what other options this command accepts."""

    maxvmOptions = os.getenv('MAXVM_OPTIONS', '').split()
    env.run([join(env.vmdir, 'maxvm')] + maxvmOptions + args)
            
_patternHelp="""
    A pattern is a class name pattern followed by an optional method name
    pattern separated by a ':' further followed by an optional signature:

      <class name>[:<method name>[:<signature>]]

    For example, the list of patterns:

         "Object:wait", "String", "Util:add:(int,float)"

    will match all methods in a class whose name contains "Object" where the
    method name contains "wait", all methods in a class whose name
    contains "String" and all methods in any class whose name
    contains "Util", the method name contains "add" and the
    signature is (int, float).

    The type of matching performed for a given class/method name is determined
    by the position of '^' in the pattern name as follows:

    Position of '^'   | Match algorithm
     ------------------+------------------
     start AND end     | Equality
     start             | Prefix
     end               | Suffix
     absent            | Substring

    For example, "^java.util:^toString^" matches all methods named "toString" in
    any class whose name starts with "java.util".

    The matching performed on a signature is always a substring test. Signatures can
    specified either in Java source syntax (e.g. "int,String") or JVM internal syntax
    (e.g. "IFLjava/lang/String;"). The latter must always use fully qualified type
    names where as the former must not.

    Any pattern starting with "!" is an exclusion specification. Any class or method
    whose name contains an exclusion string (the exclusion specification minus the
    leading "!") is excluded."""

def _vm_image(env):
    return join(env.vmdir, 'maxine.vm')
    
# Table of commands in alphabetical order.
# Keys are command names, value are lists: [<function>, <usage msg>, <format args to doc string of function>...]
# If any of the format args are instances of Callable, then they are called with an 'env' are before being
# used in the call to str.format().  
# Extensions should update this table directly
table = {
    'build': [build, '[options] projects...'],
    'c1x': [c1x, '[options] patterns...'],
    'check': [check, 'projects...'],
    'canonicalizeprojects': [canonicalizeprojects, ''],
    'clean': [clean, ''],
    'configs': [configs, ''],
    'copycheck': [copycheck, ''],
    'eclipseprojects': [eclipseprojects, ''],
    'gate': [gate, '[options]'],
    'graal': [graal, '[options] patterns...'],
    'hcfdis': [hcfdis, '[options] files...'],
    'helloworld': [helloworld, '[VM options]'],
    'help': [help_, '[command]'],
    'image': [image, '[options] classes|packages...'],
    'inspect': [inspect, '[options] [class | -jar jarfile]  [args...]'],
    'jnigen': [jnigen, ''],
    'jttgen': [jttgen, ''],
    'methodtree': [methodtree, '[options]'],
    'nm': [nm, '[options] [boot image file]', _vm_image],
    'objecttree': [objecttree, '[options]'],
    'olc': [olc, '[options] patterns...', _patternHelp],
    'projects': [projects, '', _patternHelp],
    't1x': [t1x, '[options] patterns...'],
    't1xgen': [t1xgen, ''],
    'test': [test, '[options]'],
    'verify': [verify, '[options] patterns...', _patternHelp],
    'vm': [vm, '[options] [class | -jar jarfile]  [args...]']
}