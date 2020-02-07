#
# commands.py - the Maxine commands extensions to mx
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2017-2019, APT Group, School of Computer Science,
# The University of Manchester. All rights reserved.
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
# ----------------------------------------------------------------------------------------------------

from __future__ import print_function
import os, shutil, fnmatch, subprocess, platform, itertools, datetime, sys, csv, re, multiprocessing
from os.path import join, exists, dirname, isdir, pathsep, isfile
import mx
from argparse import ArgumentParser

_maxine_home = dirname(dirname(__file__))
_vmdir = None

# Set LD_LIBRARY_PATH to make dlopen work
#
# `DT_RPATH` is deprecated and replaced by `DT_RUNPATH`.  However
# `DT_RUNPATH` is not transitive and thus it's not propagated through
# maxvm to `dlopen` when loading `libjava.so`.  As a result, when
# loading `libjava.so` and chain-loading `libjvm.so` MaxineVM fails
# (unless `libjava.so` sets `DT_RUNPATH`, which it should NOT anymore).
# To overcome this issue we set `LD_LIBRARY_PATH`
# accordingly. Consequently the use of `-rpath` when linking maxvm (see
# `com.oracle.max.vm.native/platform/platform.mk`) is redundant now.
ldenv = os.environ
platform = platform.system()
if platform == "Linux":
    ldenv['LD_LIBRARY_PATH'] = ldenv['MAXINE_HOME'] + "/com.oracle.max.vm.native/generated/" + mx.get_os()
elif platform == "Darwin":
    ldenv['LD_LIBRARY_PATH'] = ldenv['MAXINE_HOME'] + "/com.oracle.max.vm.native/build/" + mx.get_os() + "/substrate"


def c1x(args):
    """alias for "mx olc -c=C1X ..." """
    olc(['-c=C1X'] + args)


def _configs():
    class Configs:
        def __init__(self):
            self.configs = dict()

        def eat(self, line):
            (k, v) = line.split('#')
            self.configs[k] = v.rstrip()

    c = Configs()
    mx.run([mx.get_jdk().java, '-client', '-Xmx40m', '-Xms40m', '-XX:NewSize=30m', '-cp', mx.classpath(resolve=False),
            'com.oracle.max.vm.tests.vm.MaxineTesterConfiguration'], out=c.eat)
    return c.configs


def configs(arg):
    """prints the predefined image configurations"""
    c = _configs()
    mx.log('The available preconfigured option sets are:')
    mx.log()
    mx.log('    Configuration    Expansion')
    for k, v in sorted(c.iteritems()):
        mx.log('    @{0:<16} {1}'.format(k, v.replace('@', ' ')))


def checkcopyrights(args):
    """run copyright check on the Maxine sources"""
    for i in args:
        if i == '-h' or i == '-help' or i == '--help':
            print('for help run mx :checkcopyrights -h')
            return
    mx.checkcopyrights(['--', '--copyright-dir', 'mx.maxine'] + args)


def build(args):
    for i in args:
        if i == '-h' or i == '-help' or i == '--help':
            print('for help run mx :build -h')
            return
    mx.build(['--warning-as-error'] + args)


def eclipse(args):
    """launch Eclipse with the Maxine VM

    Run Eclipse with the Maxine VM, by-passing the native Eclipse launcher.
    The ECLIPSE_HOME environment variable must be set and point to
    the parent of the 'plugins' directory in an Eclipse installation."""

    # see http://wiki.eclipse.org/FAQ_How_do_I_run_Eclipse%3F
    # and http://wiki.eclipse.org/Starting_Eclipse_Commandline_With_Equinox_Launcher

    eclipse = os.environ.get('ECLIPSE_HOME')
    if eclipse is None:
        mx.abort('The ECLIPSE_HOME environment variable must be set')
    plugins = join(eclipse, 'plugins')
    if not exists(plugins):
        mx.abort(
            'The ECLIPSE_HOME variable must denote the parent of the "plugins" directory in an Eclipse installation')

    launchers = fnmatch.filter(os.listdir(plugins), 'org.eclipse.equinox.launcher_*.jar')
    if len(launchers) == 0:
        mx.abort('Could not find org.eclipse.equinox.launcher_*.jar in ' + plugins)

    launcher = join(plugins, sorted(launchers)[0])
    return mx.run([join(_vmdir, 'maxvm'), '-Xms1g', '-Xmx3g', '-XX:+ShowConfiguration'] + args + ['-jar', launcher],
                  env=ldenv)


def gate(args):
    """run the tests used to validate a push to the stable Maxine repository

    If this commands exits with a 0 exit code, then the source code is in
    a state that would be accepted for integration into the main repository."""

    check = True
    testArgs = []

    i = 0
    while i < len(args):
        arg = args[i]
        if arg == '-nocheck':
            check = False
        else:
            testArgs += [arg]
        i += 1

    mx._opts.specific_suites = ["maxine"]
    if check:
        if mx.checkstyle([]):
            mx.abort('Checkstyle warnings were found')

        if exists(join(_maxine_home, '.git')):
            # Copyright check depends on the sources being in a git repo
            mx.log('Running checkcopyrights')
            if checkcopyrights(['--modified', '--report-errors']):
                mx.abort('Copyright issues were found')

    mx.log('Ensuring JavaTester harness is up to date')
    try:
        jttgen([])
    except SystemExit:
        mx.log('Updated JavaTesterRunScheme.java or JavaTesterTests.java in com.sun.max.vm.jtrun.all.')
        mx.log('To push your changes to the repository, these files need to be generated locally and checked in.')
        mx.log('The files can be generated by running: mx jttgen')
        mx.abort(1)

    mx.log('Ensuring mx/suite.py files are canonicalized')
    try:
        mx.canonicalizeprojects([])
    except SystemExit:
        mx.log('Rerun "mx canonicalizeprojects" and check-in the modified mx/suite.py files.')
        mx.abort(1)

    mx.log('Running MaxineTester...')

    testme(['-image-configs=java',
            '-maxvm-configs=std,forceC1X,forceT1X',
            '-jtt-image-configs=jtt-c1xc1x,jtt-t1xc1x,jtt-c1xt1x,jtt-t1xt1x',
            '-tests=c1x,junit:uk.ac+tests.unsafe+tests.vm+max.l+max.c+max.u+max.i+max.M+max.p,jsr292,output,javatester'] + testArgs)
    testme(['-image-configs=ss', '-tests=output:Hello+Catch+GC+WeakRef+Final', '-fail-fast'] + testArgs)


def gitinit(args):
    """
    Setup git commit template and pre-commit/pre-push hooks
    """
    subprocess.call(["git", "config", "--local", "commit.template", "git/commit-template"])
    subprocess.call(["git", "config", "--local", "core.hooksPath", "git/hooks"])


def hcfdis(args):
    """disassembles HexCodeFiles embedded in text files

    Run a tool over the input files to convert all embedded HexCodeFiles
    to a disassembled format."""
    mx.run_java(['-cp', mx.classpath('com.oracle.max.hcfdis'), 'com.oracle.max.hcfdis.HexCodeFileDis'] + args)


def helloworld(args):
    """run the 'hello world' program on the Maxine VM"""
    mx.run([join(_vmdir, 'maxvm'), '-cp', mx.classpath('test')] + args + ['test.output.HelloWorld'], env=ldenv)


def inspecthelloworld(args):
    """run the 'hello world' program in the Inspector"""
    inspect(['-cp', mx.classpath('test')] + args + ['test.output.HelloWorld'])


def image(args):
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
    max.platform    | name of a preset platform     | solaris-amd64 linux-amd64 darwin-amd64 linux-aarch64
    max.cpu         | processor model               | AMD64 IA32 SPARCV9 ARMV7 Aarch64
    max.isa         | instruction set architecture  | AMD64 ARM PPC SPARC Aarch64
    max.os          | operating system              | Darwin Linux Solaris
    max.endianness  | endianness                    | BIG LITTLE
    max.bits        | machine word size             | 64 32
    max.page        | page size                     | 4096 8192
    max.nsig        | number of signals             | 32
    mas.idiv        | has hw integer divider        | 1/0

    These system properties can be specified as options to the image
    command (e.g. '-os Darwin -bits 32').

    An option starting with '@' denotes one of the preconfigured set of
    options described by running "mx options".

    An option starting with '--' is interpreted as a VM option of the same name
    after the leading '-' is removed. For example, to use the '-verbose:class'
    VM option to trace class loading while image building, specify '--verbose:class'.
    Note that not all VM options have an effect during image building.

    Use "mx image -help" to see what other options this command accepts."""

    systemProps = ['-Xmx1G']
    imageArgs = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg[0] == '@':
            name = arg.lstrip('@')
            configs = _configs()
            if not name in configs:
                mx.log()
                mx.abort('Invalid image configuration: ' + name)
            if "graal" in name:
                systemProps += ['-ea', '-esa']
            values = configs[name].split('@')
            del args[i]
            args[i:i] = values
            continue
        elif arg in ['-platform', '-cpu', '-isa', '-os', '-endianness', '-bits', '-page', '-nsig', '-idiv']:
            name = arg.lstrip('-')
            i += 1
            if i == len(args):
                mx.abort('Missing value for ' + arg)
            value = args[i]
            systemProps += ['-Dmax.' + name + '=' + value]
        elif arg.startswith('--XX:LogFile='):
            os.environ['MAXINE_LOG_FILE'] = arg.split('=', 1)[1]
        elif arg.startswith('--XX:+PrintCFGToFile'):
            os.environ['PRINT_CFG'] = '1'
        elif arg.startswith('--XX:+EnableBootImageDebugMethodID'):
            os.environ['ENABLE_DEBUG_METHODS_ID'] = '1'
        elif arg.startswith('--XX:+PrintHIR'):
            os.environ['PRINT_HIR'] = '1'
        elif arg.startswith('--XX:PrintFilter='):
            os.environ['PRINT_FILTER'] = arg.split('=', 1)[1]
        elif arg == '-vma':
            systemProps += ['-Dmax.permsize=2']
        else:
            imageArgs += [arg]
        i += 1

    mx.run_java(['-Xbootclasspath/a:' + mx.distribution('GRAAL').path] + systemProps + ['-cp', suite_classpath(),
                                                                                        'com.sun.max.vm.hosted.BootImageGenerator',
                                                                                        '-trace=1',
                                                                                        '-run=java'] + imageArgs)


def check_cwd_change(args):
    """Return the current working directory having checked if it is overriden in args"""
    cwd = os.getcwd()
    vmArgs = []

    i = 0
    while i < len(args):
        arg = args[i]
        if arg == '-cwd':
            cwd = args[i + 1]
            i += 1
        else:
            vmArgs += [arg]
        i += 1
    return [cwd, vmArgs]


def suite_classpath():
    """Get the classpath containing only the current suite's dependencies, removing Graal projects (if any) from it"""
    dependencies = mx.dependencies(True)
    dependencies = itertools.ifilter(lambda d: not d.isNativeProject(), dependencies)
    dependencies = itertools.ifilter(lambda d: not d.isPackedResourceLibrary(), dependencies)
    dependencies = [ d.name for d in dependencies]
    cp = mx.classpath(dependencies)
    cp_list = cp.split(os.pathsep)
    sanitized_list = []
    for entry in cp_list:
        include = True
        if entry.find("com.oracle.graal") >= 0 or entry.find("com.oracle.truffle") >= 0 or entry.find(
                "graal/dists") >= 0:
            include = False
        if include:
            sanitized_list.append(entry)
    result = os.pathsep.join(sanitized_list)
    return result

def inspect(args):
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

    saveClassDir = join(_vmdir, 'inspected_classes')
    maxvmOptions = os.getenv('MAXVM_OPTIONS', '').split()
    vmArgs = ['-XX:SaveClassDir=' + saveClassDir, '-XX:+TrapOnError'] + maxvmOptions
    insArgs = ['-vmdir=' + _vmdir]
    if not isdir(saveClassDir):
        os.makedirs(saveClassDir)
    sysProps = []
    sysProps += ['-Xbootclasspath/a:' + mx.distribution('GRAAL').path]
    insCP = []

    cwdArgs = check_cwd_change(args)
    cwd = cwdArgs[0]
    args = cwdArgs[1]

    i = 0
    remote = False
    while i < len(args):
        arg = args[i]
        if arg.startswith('-XX:LogFile='):
            logFile = arg.split('=', 1)[1]
            vmArgs += [arg]
            os.environ['TELE_LOG_FILE'] = 'tele-' + logFile
        elif arg in ['-cp', '-classpath']:
            vmArgs += [arg, args[i + 1]]
            insCP += [mx.expand_project_in_class_path_arg(args[i + 1])]
            i += 1
        elif arg == '-jar':
            vmArgs += ['-jar', args[i + 1]]
            insCP += [args[i + 1]]
            i += 1
        elif arg == '--remote':
            remote = True
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

    mx.expand_project_in_args(vmArgs)

    cmd = [mx.get_jdk().java]
    cmd += mx.get_jdk().processArgs(
        sysProps + ['-cp', suite_classpath() + pathsep + insCP, 'com.sun.max.ins.MaxineInspector'] +
        insArgs + ['-a=' + ' '.join(vmArgs)])

    if mx.is_darwin() and not remote:
        # The -E option propagates the environment variables into the sudo process
        mx.run(['sudo', '-E', '-p',
                'Debugging is a privileged operation on Mac OS X. Please enter your "sudo" password:'] + cmd, cwd=cwd)
    else:
        mx.run(cmd, cwd=cwd, env=ldenv)


def inspectoragent(args):
    """launch the Inspector agent

    Launch the Inspector agent.

    The agent listens on a given port for an incoming connection from
    a remote Inspector process."""

    cmd = [mx.get_jdk().java]
    cmd += mx.get_jdk().processArgs(['-cp', mx.classpath(), 'com.sun.max.tele.channel.agent.InspectorAgent'] + args)
    if mx.is_darwin():
        # The -E option propagates the environment variables into the sudo process
        mx.run(['sudo', '-E', '-p',
                'Debugging is a privileged operation on Mac OS X.\nPlease enter your "sudo" password:'] + cmd)
    else:
        mx.run(cmd, env=ldenv)


def jnigen(args):
    """(re)generate Java source for native function interfaces (i.e. JNI, JMM, VM)

    Run JniFunctionsGenerator.java to update the methods in [Jni|JMM|VM]Functions.java
    by adding a prologue and epilogue to the @VM_ENTRY_POINT annotated methods in
    [Jni|JMM|VM]FunctionsSource.java.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath('com.sun.max'), 'com.sun.max.vm.jni.JniFunctionsGenerator'])


def optionsgen(args):
    """(re)generate Java source for Graal Options"""

    return mx.run_java(['-ea', '-cp', mx.classpath('com.oracle.max.vm.ext.graal'),
                        'com.oracle.max.vm.ext.graal.hosted.MaxGraalOptionsGenerator'] + args)


def jvmtigen(args):
    """(re)generate Java source for JVMTI native function interfaces

    Run JniFunctionsGenerator.java to update the methods in JVMTIFunctions.java
    by adding a prologue and epilogue to the @VM_ENTRY_POINT annotated methods in
    JVMTIFunctionsSource.java.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm.ext.jvmti', jdk=mx.get_jdk()),
                        'com.oracle.max.vm.ext.jvmti.JVMTIFunctionsGenerator'])


def jjvmtigen(args):
    """(re)generate Java source for JJVMTI native function interfaces

    Run JniFunctionsGenerator.java to update the methods in JVMTIFunctions.java
    by adding a prologue and epilogue to the @VM_ENTRY_POINT annotated methods in
    JVMTIFunctionsSource.java.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm.ext.jvmti', jdk=mx.get_jdk()),
                        'com.oracle.max.vm.ext.jvmti.JJVMTIAgentAdapterChecker'])


def jttgen(args):
    """(re)generate harness and run scheme for the JavaTester tests

    Run the JavaTester to update the JavaTesterRunScheme.java and JavaTesterTests.java
    files in the com.sun.max.vm.jtrun.all package."""

    testDirs = [join(mx.project('jtt').dir, 'src')]
    tests = []
    for testDir in testDirs:
        for name in os.listdir(join(testDir, 'jtt')):
            if name != 'hotspot' and name != 'fail':
                tests.append(join(testDir, 'jtt', name))
    return mx.run_java(
        ['-cp', mx.classpath('com.oracle.max.vm.tests'), 'com.oracle.max.vm.tests.vm.compiler.JavaTester',
         '-scenario=target', '-run-scheme-package=all', '-native-tests'] + tests)


def loggen(args):
    """(re)generate Java source for VMLogger interfaces

    Run VMLoggerGenerator.java to update the Auto implementations of @VMLoggerInterface interfaces.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath(['com.sun.max', 'com.oracle.max.vm.ext.jvmti',
                                             'com.oracle.max.vm.ext.vma']),
                        'com.sun.max.vm.log.hosted.VMLoggerGenerator'])


def makejdk(args):
    """create a JDK directory based on the Maxine VM

    Create a JDK directory by replicating the file structure of $JAVA_HOME
    and replacing the 'java' executable with the Maxine VM
    executable. This produces a Maxine VM based JDK for applications
    (such as NetBeans) which expect a certain directory structure
    and executable names in a JDK installation."""

    if mx.is_darwin():
        mx.log('mx makejdk is not supported on Darwin')
        mx.abort(1)

    if len(args) == 0:
        maxjdk = join(_maxine_home, 'maxjdk')
    else:
        maxjdk = args[0]
        if maxjdk[0] != '/':
            maxjdk = join(os.getcwd(), maxjdk)

    if exists(maxjdk):
        mx.log('The destination directory already exists -- it will be deleted')
        shutil.rmtree(maxjdk)

    jdk = mx.get_jdk().home
    if not isdir(jdk):
        mx.log(jdk + " does not exist or is not a directory")
        mx.abort(1)

    mx.log('Replicating ' + jdk + ' in ' + maxjdk + '...')
    shutil.copytree(jdk, maxjdk, symlinks=True)

    jreExists = exists(join(maxjdk, 'jre'))

    for f in os.listdir(_vmdir):
        fpath = join(_vmdir, f)
        if isfile(fpath):
            shutil.copy(fpath, join(maxjdk, 'bin'))
            if jreExists:
                shutil.copy(fpath, join(maxjdk, 'jre', 'bin'))

    os.unlink(join(maxjdk, 'bin', 'java'))
    if jreExists:
        os.unlink(join(maxjdk, 'jre', 'bin', 'java'))
    if (mx.os == 'windows'):
        shutil.copy(join(maxjdk, 'bin', 'maxvm'), join(maxjdk, 'bin', 'java'))
        shutil.copy(join(maxjdk, 'jre', 'bin', 'maxvm'), join(maxjdk, 'jre', 'bin', 'java'))
    else:
        os.symlink(join(maxjdk, 'bin', 'maxvm'), join(maxjdk, 'bin', 'java'))
        if jreExists:
            os.symlink(join(maxjdk, 'jre', 'bin', 'maxvm'), join(maxjdk, 'jre', 'bin', 'java'))

    mx.log('Created Maxine based JDK in ' + maxjdk)


def methodtree(args):
    """print the causality spanning-tree of the method graph in the boot image

    The causality spanning-tree allows one to audit the boot image with respect
    to why any given method is (or isn't) in the image. This is useful when
    trying to reduce the size of the image.

    This tool requires an input *.tree file which is produced by specifying the
    -tree option when building the boot image.

    Use "mx methodtree -help" to see what other options this command accepts."""

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImageMethodTree',
                 '-in=' + join(_vmdir, 'maxine.method.tree')] + args)


def nm(args):
    """print the contents of a boot image

    Print the contents of a boot image in a textual form.
    If not specified, the following path will be used for the boot image file:

        {0}

    Use "mx nm -help" to see what other options this command accepts."""

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImagePrinter'] + args + [join(_vmdir, 'maxine.vm')])


def objecttree(args):
    """print the causality spanning-tree of the object graph in the boot image

    The causality spanning-tree allows one to audit the boot image with respect
    to why any given object is in the image. This is useful when trying to reduce
    the size of the image.

    This tool requires an input *.tree file which is produced by specifying the
    -tree option when building the boot image.

    Use "mx objecttree -help" to see what other options this command accepts."""

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImageObjectTree',
                 '-in=' + join(_vmdir, 'maxine.object.tree')] + args)


def olc(args):
    """offline compile a list of methods

    See Patterns below for a description of the format expected for "patterns..."

    The output traced by this command is not guaranteed to be the same as the output
    for a compilation performed at runtime. The code produced by a compiler is sensitive
    to the compilation context such as what classes have been resolved etc.

    Use "mx olc -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    i = 0
    insCP = []
    olcArgs = []

    while i < len(args):
        arg = args[i]
        if arg in ['-cp', '-classpath']:
            insCP += [mx.expand_project_in_class_path_arg(args[i + 1])]
            i += 1
        else:
            olcArgs += [arg]
        i += 1

    insCP = pathsep.join(insCP)
    mx.run_java(['-ea', '-esa', '-cp', mx.classpath() + pathsep + insCP, 'com.oracle.max.vm.ext.maxri.Compile'] + olcArgs)

def getEntryOrExitPoint(option, vmArgs):
    index = vmArgs.index(option)
    value = vmArgs[index+1]
    del vmArgs[index+1]
    del vmArgs[index]
    value = '"'+value.split('(')[0]+'"'
    if option is 'entry':
        return '-XX:AllocationProfilerEntryPoint='+value
    else:
        return '-XX:AllocationProfilerExitPoint='+value

def ignoreTheRestOptions(vmArgs, profilerOptions):
    for arg in reversed(vmArgs):
        if arg in profilerOptions:
            if arg == 'entry' or arg == 'exit':
                del vmArgs[vmArgs.index(arg)+1]
            del vmArgs[vmArgs.index(arg)]

def numaProfilerOutputProcessing(filename):
    if type(filename) == list:
        oldFileName = os.path.abspath(filename[0])
    else:
        oldFileName = os.path.abspath(filename)

    #extract the benchmark name
    #we assume that the file name follows the following format
    #benchmark_numaprofiler_out.csv
    benchmark = os.path.basename(oldFileName).split('.')[0].split('_')[0]
    path = os.path.dirname(oldFileName)

    #open the profiler output file
    oldFile = open(oldFileName, 'r')
    lines = oldFile.readlines()
    oldFile.close

    #open a new file for Object Allocations
    objectAllocationsFileName = path+'/tmp.csv'
    objectAllocationsFile = open(objectAllocationsFileName, 'a')
    # Object Allocations File Header
    objectAllocationsFile.write('Cycle;isAllocation;UniqueId;ThreadId;ThreadNumaNode;Type;Size;NumaNode;Timestamp;CoreID\n')

    #open a new file for Heap Boundaries
    heapBoundariesFileName = path+'/'+benchmark+'_heap_boundaries.csv'
    heapBoundariesNewFile = open(heapBoundariesFileName, 'w')
    # Heap Boundaries File Header
    heapBoundariesNewFile.write('Cycle;NumaNode;NumOfPages\n')

    #open a new file for Object Accesses
    objectAccessesFileName = path+'/'+benchmark+'_object_accesses.csv'
    objectAccessesFile = open(objectAccessesFileName, 'w')
    # Object Accesses File Header
    objectAccessesFile.write('Cycle;ThreadId;AccessType;Count\n')

    #object allocations regex
    #Cycle ; is New Allocation ; ID ; Thread id ; Class/Type ; Size ; NUMA Node ; Timestamp
    objectAllocationPattern = r'[0-9]+\;[0-9]+\;[0-9]+\;([0-9]+)\;[0-9]+\;[^\;.]*\;[0-9]+\;[0-9]+\;[0-9]+\;[0-9]'

    #heap boundaries regex
    heapBoundariesPattern = r'\(heapBoundaries\)+\;[0-9]+\;[0-9]+\;[0-9]'

    #access counter regex
    #(accessCounter);Cycle;ThreadId;CounterName;Count
    objectAccessPattern = r'\(accessCounter\)+.'


    print('\n=> Processing NUMAProfiler\'s Output:')
    print('Generating Object Allocation Trace, Heap Boundaries Trace and Object Access Trace...')
    for line in lines:
        objectAllocationLineMatch = re.match(objectAllocationPattern, line)
        heapBoundariesLineMatch = re.match(heapBoundariesPattern, line)
        accessCounterLineMatch = re.match(objectAccessPattern, line)

        if (objectAllocationLineMatch):
            # Allocation Profiler Output line found
            index = int(objectAllocationLineMatch.group(1))
            fields = line.split(';')

            cycle = fields[0]
            isAllocation = fields[1]
            uniqueId = fields[2]
            threadId = fields[3]
            threadNumaNode = fields[4]
            classOrType = fields[5]
            size = fields[6]
            numaNode = fields[7]
            timestamp = fields[8]
            coreid = fields[9]

            # Create the New Line
            newLine = cycle + ';' + isAllocation + ';' + uniqueId + ';' + threadId + ';' + threadNumaNode + ';' + classOrType + ';' + size + ';' + numaNode + ';' + timestamp + ';' + coreid
            objectAllocationsFile.write(newLine)

        elif (heapBoundariesLineMatch):
            # Heap Boundaries line found
            fields = line.split(';')
            cycle = fields[1]
            numaNode = fields[2]
            numOfPages = fields[3]
            #write on the heap boundaries file
            hbNewLine = cycle + ';' + numaNode + ';' + numOfPages
            heapBoundariesNewFile.write(hbNewLine + '\n')

        elif (accessCounterLineMatch):
            fields = line.split(';')
            cycle = fields[1]
            threadId = fields[2]
            counterName = fields[3]
            count = int(fields[4])
            accessesNewLine = cycle + ';' + threadId + ';' + counterName + ';' + str(count)
            objectAccessesFile.write(accessesNewLine + '\n')
            
    objectAllocationsFile.close
    heapBoundariesNewFile.close
    objectAccessesFile.close

    # delete the old output
    os.unlink(oldFileName)
    # replace with the new output
    shutil.move(objectAllocationsFileName, oldFileName)
    
    print('The Output Processing is Finished.\n')
    print('=> Results directory:')
    print('a)' + oldFileName + '\nb)' + heapBoundariesFileName + '\nb)' + objectAccessesFileName)

def numaprofiler(args):
    """launch Maxine VM with NUMA Profiler

    Run the Maxine VM with the NUMA Profiler and the given options and arguments.

    where options include:
        all                                                         profile the all allocated objects
        bufferSize                                                  the profiler's buffer size.
        entry <entry point method> [ | exit <exit point method> ]   profile the allocated objects from the entry until the exit method. If no exitpoint given profiles until the end.
        log                                                         execute the application and log the compiled methods by C1X and T1X. 1st priority if present.
        verbose                                                     enable NUMA profiler's verbosity.
        warmup <num>                                                num of iterations to be considered as warmup and consequently to be ignored.

    If no option given will run with the -XX:+NUMAProfilerAll option. This will profile all objects.

    Use "mx numaprofiler -help" to see what other options this command accepts."""

    cwdArgs = check_cwd_change(args)
    cwd = cwdArgs[0]
    vmArgs = cwdArgs[1]

    profilerArgs = []

    profilerOptions = ['all', 'entry', 'exit', 'log', 'verbose']

    if 'log' in vmArgs:
        profilerArgs.append('-XX:+LogCompiledMethods')
        #ignore the rest profiler options
        ignoreTheRestOptions(vmArgs, profilerOptions)
    else:
        
        # entry | entry exit
        if 'entry' in vmArgs:
            profilerArgs.append(getEntryOrExitPoint('entry', vmArgs))
            if 'exit' in vmArgs:
                profilerArgs.append(getEntryOrExitPoint('exit', vmArgs))

        #enable/disable verbosity
        if 'verbose' in vmArgs:
            del vmArgs[vmArgs.index('verbose')]
            profilerArgs.append('-XX:+NUMAProfilerVerbose')

        if 'warmup' in vmArgs:
            index = vmArgs.index('warmup')
            num = vmArgs[index+1]
            del vmArgs[index+1]
            del vmArgs[index]
            profilerArgs.append('-XX:NUMAProfilerExplicitGCThreshold='+num)
        elif 'all' in vmArgs:
            index = vmArgs.index('all')
            del vmArgs[index]
            profilerArgs.append('-XX:NUMAProfilerExplicitGCThreshold=0')

        if 'buffersize' in vmArgs:
            index = vmArgs.index('buffersize')
            num = vmArgs[index+1]
            del vmArgs[index+1]
            del vmArgs[index]
            profilerArgs.append('-XX:NUMAProfilerBufferSize='+num)

        if 'validate' in vmArgs:
            del vmArgs[vmArgs.index('validate')]
            profilerArgs.append('-XX:+NUMAProfilerDebug')

    print('==================================================')
    print('== Launching Maxine VM with NUMA Profiler ==')
    print('==================================================')
    print('== Profiler Args:')
    for args in profilerArgs:
        print('\t', args)
    print('== VM and Application Args:')
    for args in vmArgs:
        print('\t', args)
    print('==================================================')

    mx.run([join(_vmdir, 'maxvm')] + profilerArgs + vmArgs, cwd=cwd, env=ldenv)

    print('==================================================')
    print('The execution is finished.')
    numaProfilerOutputProcessing(os.path.basename(os.getenv('MAXINE_LOG_FILE')))

def site(args):
    """creates a website containing javadoc and the project dependency graph"""

    return mx.site(['--name', 'Maxine',
                    '--exclude-packages', 'com.sun.max.asm.amd64.complete,com.sun.max.asm.ia32.complete',
                    '--overview', join(_maxine_home, 'overview.html'),
                    '--title', 'Maxine Project Documentation',
                    '--dot-output-base', 'modules'] + args)


def t1x(args):
    """alias for "mx olc -c=T1X ..." """
    olc(['-c=T1X'] + args)


def t1xgen(args):
    """(re)generate content in T1XTemplateSource.java

    Run T1XTemplateGenerator.java to generate the auto-generated templates in T1XTemplateSource.java.

    The exit code is non-zero if the auto-generated part of T1XTemplateSource.java was modified."""

    return mx.run_java(
        ['-cp', mx.classpath('com.oracle.max.vm.ext.t1x'), 'com.oracle.max.vm.ext.t1x.T1XTemplateGenerator'])


def testme(args):
    """run some or all of the Maxine tests


    The Maxine sources include a variety of tests that can be run by a
    special launcher. These include JUnit tests, VM micro tests, certain
    benchmark suites and output comparison tests, amongst others.

    Use "mx test -help" to see what other options this command accepts."""
    maxineTesterDir = join(_maxine_home, 'maxine-tester')
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
            mx.log(line.rstrip())
            self.f.write(line)

    console = join(maxineTesterDir, 'console')
    with open(console, 'w', 0) as f:
        tee = Tee(f)
        jdk = mx.get_jdk()
        mx.run_java(
            ['-cp', suite_classpath(), 'com.oracle.max.vm.tests.vm.MaxineTester', '-output-dir=maxine-tester',
             '-graal-jar=' + mx.distribution('GRAAL').path,
             '-refvm=' + jdk.java, '-refvm-args=' + ' '.join(jdk.java_args)] + args, out=tee.eat, err=subprocess.STDOUT)


def verify(args):
    """verifies a set of methods using the Maxine bytecode verifier

    Run the Maxine verifier over a set of specified methods available
    on the class path. To extend the class path, use one of the global
    "--cp-pfx" or "--cp-sfx" options.

    See Patterns below for a description of the format expected for "patterns..."

    Use "mx verify -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    mx.run_java(['-cp', mx.classpath(), 'com.oracle.max.vm.tests.vm.verifier.CommandLineVerifier'] + args)


def view(args):
    """browse the boot image under the Inspector

    Browse a Maxine boot image under the Inspector.

    Use "mx view -help" to see what the Inspector options are."""

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.ins.MaxineInspector', '-vmdir=' + _vmdir, '-mode=image'] + args)


def vm(args):
    """launch the Maxine VM

    Run the Maxine VM with the given options and arguments.
    A class path component with a '@' prefix is expanded to be the
    class path of the project named after the '@'.
    The expansion of the MAXVM_OPTIONS environment variable is inserted
    before any other VM options specified on the command line.

    Use "mx vm -help" to see what other options this command accepts."""

    cwdArgs = check_cwd_change(args)
    cwd = cwdArgs[0]
    vmArgs = cwdArgs[1]

    mx.expand_project_in_args(vmArgs)
    maxvmOptions = os.getenv('MAXVM_OPTIONS', '').split()

    debug_args = mx.get_jdk().debug_args
    if debug_args is not []:
        maxvmOptions += debug_args

    mx.run([join(_vmdir, 'maxvm')] + maxvmOptions + vmArgs, cwd=cwd, env=ldenv)


_patternHelp = """
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


def _vm_image():
    return join(_vmdir, 'maxine.vm')


def wikidoc(args):
    """generate Confluence Wiki format for package-info.java files"""

    # Ensure the wiki doclet is up to date
    mx.build(['--projects', 'com.oracle.max.tools'])

    # the WikiDoclet cannot see the -classpath argument passed to javadoc so we pass the
    # full list of projects as an explicit argument, thereby enabling it to map classes
    # to projects, which is needed to generate Wiki links to the source code.
    # There is no virtue in running the doclet on dependent projects as there are
    # no generated links between Wiki pages
    toolsDir = mx.project('com.oracle.max.tools').output_dir()
    baseDir = mx.project('com.sun.max').output_dir()
    dp = os.pathsep.join([toolsDir, baseDir])
    project_list = ','.join(p.name for p in mx.sorted_deps())
    for a in ['-docletpath', dp, '-doclet', 'com.oracle.max.tools.javadoc.wiki.WikiDoclet', '-projects', project_list]:
        args.append('--arg')
        args.append('@' + a)

    mx.javadoc(args, parser=ArgumentParser('mx wikidoc'), docDir='wikidoc', includeDeps=False, stdDoclet=False)


def mx_init(suite):
    mx.add_argument('--vmdir', dest='vmdir',
                    help='directory for VM executable, shared libraries boot image and related files', metavar='<path>')

    commands = {
        'numaprofiler': [numaprofiler, ''],
        'numaProfilerOutputProcessing': [numaProfilerOutputProcessing, ''],
        'build': [build, '"for help run mx :build -h"'],
        'c1x': [c1x, '[options] patterns...'],
        'configs': [configs, ''],
        'checkcopyrights': [checkcopyrights, '"for help run mx :checkcopyrights -h"'],
        'eclipse': [eclipse, '[VM options]'],
        'gate': [gate, '[-nocheck] [args...]'],
        'gitinit': [gitinit, ''],
        'hcfdis': [hcfdis, '[options] files...'],
        'helloworld': [helloworld, '[VM options]'],
        'inspecthelloworld': [inspecthelloworld, '[VM options]'],
        'image': [image, '[options] classes|packages...'],
        'inspect': [inspect, '[options] [class | -jar jarfile]  [args...]'],
        'inspectoragent': [inspectoragent, '[-impl target] [-port port]'],
        'jnigen': [jnigen, ''],
        'jvmtigen': [jvmtigen, ''],
        'jjvmtigen': [jjvmtigen, ''],
        'optionsgen': [optionsgen, ''],
        'jttgen': [jttgen, ''],
        'loggen': [loggen, ''],
        'makejdk': [makejdk, '[<destination directory>]'],
        'methodtree': [methodtree, '[options]'],
        'nm': [nm, '[options] [boot image file]', _vm_image],
        'objecttree': [objecttree, '[options]'],
        'olc': [olc, '[-cp classpath] [options] patterns...', _patternHelp],
        'site': [site, '[options]'],
        't1x': [t1x, '[options] patterns...'],
        't1xgen': [t1xgen, ''],
        'testme': [testme, '[options]'],
        'verify': [verify, '[options] patterns...', _patternHelp],
        'view': [view, '[options]'],
        'vm': [vm, '[options] [class | -jar jarfile]  [args...]'],
        'wikidoc': [wikidoc, '[options]'],
    }
    mx.update_commands(suite, commands)


def mx_post_parse_cmd_line(opts):
    global _vmdir
    if opts.vmdir is None:
        _vmdir = join(_maxine_home, 'com.oracle.max.vm.native', 'generated', mx.get_os())
    else:
        _vmdir = opts.vmdir
