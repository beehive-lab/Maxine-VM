#
# commands.py - the Maxine commands extensions to mx
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

import os, shutil, fnmatch, subprocess
from os.path import join, exists, dirname, isdir, pathsep, isfile
import mx

_maxine_home = dirname(dirname(__file__))
_vmdir = None

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
    mx.run([mx.java().java, '-client', '-Xmx40m', '-Xms40m', '-XX:NewSize=30m', '-cp', mx.classpath(resolve=False), 'test.com.sun.max.vm.MaxineTesterConfiguration'], out=c.eat)
    return c.configs

def configs(arg):
    """prints the predefined image configurations"""
    c = _configs()
    mx.log('The available preconfigured option sets are:')
    mx.log()
    mx.log('    Configuration    Expansion')
    for k, v in sorted(c.iteritems()):
        mx.log('    @{0:<16} {1}'.format(k, v.replace(',', ' ')))
    
def copycheck(args):
    """run copyright check on the Maxine sources (defined as being under hg control)"""
    mx.build(['--projects', 'com.oracle.max.base'])
    mx.run_java(['-cp', mx.classpath('com.oracle.max.base', resolve=False), 'com.sun.max.tools.CheckCopyright'] + args)

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
        mx.abort('The ECLIPSE_HOME variable must denote the parent of the "plugins" directory in an Eclipse installation')
        
    launchers = fnmatch.filter(os.listdir(plugins), 'org.eclipse.equinox.launcher_*.jar')
    if len(launchers) == 0: 
        mx.abort('Could not find org.eclipse.equinox.launcher_*.jar in ' + plugins)

    launcher = join(plugins, sorted(launchers)[0])
    return mx.run([join(_vmdir, 'maxvm'), '-Xms1g', '-Xmx3g', '-XX:+ShowConfiguration'] + args + ['-jar', launcher])
    
def gate(args):
    """run the tests used to validate a push to the stable Maxine repository

    If this commands exits with a 0 exit code, then the source code is in
    a state that would be accepted for integration into the main repository."""
    
    if mx.checkstyle([]):
        mx.abort('Checkstyle warnings were found')
    
    if exists(join(_maxine_home, '.hg')):
        # Copyright check depends on the sources being in a Mercurial repo
        mx.log('Running copycheck')
        hgNode = os.getenv('hg_node')
        if hgNode is None:
            copycheck(['-modified', '-reporterrors=true', '-continueonerror'])
        else:
            revTip = int(subprocess.check_output(['hg', 'tip', '--template', "'{rev}'"]).strip("'"))
            revLast = int(subprocess.check_output(['hg', 'log', '-r', hgNode, '--template', "'{rev}'"]).strip("'"))
            changesetCount = revTip - revLast + 1
            copycheck(['-last=' + str(changesetCount), '-reporterrors=true', '-continueonerror'])

    mx.log('Ensuring JavaTester harness is up to date')
    try:
        jttgen([])
    except SystemExit:
        mx.log('Updated JavaTesterRunScheme.java or JavaTesterTests.java in com.sun.max.vm.jtrun.all.')
        mx.log('To push your changes to the repository, these files need to be generated locally and checked in.')
        mx.log('The files can be generated by running: mx jttgen')
        mx.abort(1)

    mx.log('Ensuring mx/projects files are canonicalized')
    try:
        mx.canonicalizeprojects([])
    except SystemExit:
        mx.log('Rerun "mx canonicalizeprojects" and check-in the modified mx/projects files.')
        mx.abort(1)

    mx.log('Running MaxineTester...')

    test(['-image-configs=java', '-fail-fast'] + args)
    
def graal(args):
    """alias for "mx olc -c=Graal ..." """
    olc(['-c=Graal'] + args)

def graalexample(args):
    """run some or all Graal examples in Maxine (assumes VM was built with @graal config)"""

    # name -> (project, main class, -XX:CompiledCommand match string)
    examples = {
        'safeadd': ['com.oracle.max.graal.examples.safeadd', 'com.oracle.max.graal.examples.safeadd.Main', 'safeadd'],
        'vectorlib': ['com.oracle.max.graal.examples.vectorlib', 'com.oracle.max.graal.examples.vectorlib.Main', 'vectorlib'],
    }

    def run_example(verbose, project, mainClass, match):
        cp = mx.classpath(project)
        sharedArgs = [mainClass]
        
        hsPrintArg = '-XX:+PrintCompilation' if verbose else '-XX:-PrintCompilation'
        c1xPrintArg = '-C1X:+PrintCompilation' if verbose else '-C1X:-PrintCompilation'
        graalPrintArg = '-G:+PrintCompilation' if verbose else '-G:-PrintCompilation'
        
        res = []
        mx.log("=== HotSpot Server ===")
        res.append(mx.run(['java', '-server', '-XX:CompileOnly=Main', '-cp', cp, hsPrintArg] + sharedArgs))
        mx.log("=== Maxine C1X ===")
        res.append(mx.run([join(_vmdir, 'maxvm'), '-XX:CompileCommand=' + match + ':C1X', '-cp', cp, c1xPrintArg, '-G:-Extend', '-G:-Inline'] + sharedArgs))
        mx.log("=== Maxine Graal ===")
        res.append(mx.run([join(_vmdir, 'maxvm'), '-XX:CompileCommand=' + match + ':Graal', '-cp', cp, graalPrintArg, '-G:-Extend', '-G:-Inline'] + sharedArgs))
        mx.log("=== Maxine Graal with extensions ===")
        res.append(mx.run([join(_vmdir, 'maxvm'), '-XX:CompileCommand=' + match + ':Graal', '-cp', cp, graalPrintArg, '-G:+Extend', '-G:-Inline'] + sharedArgs))
        
        if len([x for x in res if x != 0]) != 0:
            return 1
        return 0

    verbose = False
    if '-v' in args:
        verbose = True
        args = [a for a in args if a != '-v']

    if len(args) == 0:
        args = examples.keys()
    for a in args:
        config = examples.get(a)
        if config is None:
            mx.log('unknown example: ' + a + '  {available examples = ' + str(examples.keys()) + '}')
        else:
            mx.log('--------- ' + a + ' ------------')
            project, mainClass, match = config
            run_example(verbose, project, mainClass, match)

def hcfdis(args):
    """disassembles HexCodeFiles embedded in text files

    Run a tool over the input files to convert all embedded HexCodeFiles
    to a disassembled format."""
    mx.run_java(['-cp', mx.classpath('com.oracle.max.hcfdis'), 'com.oracle.max.hcfdis.HexCodeFileDis'] + args)

def helloworld(args):
    """run the 'hello world' program on the Maxine VM"""
    mx.run([join(_vmdir, 'maxvm'), '-cp', mx.classpath('com.oracle.max.tests')] + args + ['test.output.HelloWorld'])

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
            configs = _configs()
            if not name in configs:
                mx.log('Invalid image configuration: ' + name)
                help(['image'])
                mx.abort()
            values = configs[name].split(',')
            del args[i]
            args[i:i] = values
            continue
        elif arg in ['-platform', '-cpu', '-isa', '-os', '-endianness', '-bits', '-page', '-nsig']:
            name = arg.lstrip('-')
            i += 1
            if i == len(args):
                mx.log('Missing value for ' + arg)
                help(['image'])
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

    mx.run_java(systemProps + ['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImageGenerator', '-trace=1', '-run=java'] + imageArgs)

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
    insCP = []
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

    cmd = mx.java().format_cmd(sysProps + ['-cp', mx.classpath() + pathsep + insCP, 'com.sun.max.ins.MaxineInspector'] +
                              insArgs + ['-a=' + ' '.join(vmArgs)])
    
    
    if mx.get_os() == 'darwin' and not remote:
        # The -E option propagates the environment variables into the sudo process
        mx.run(['sudo', '-E', '-p', 'Debugging is a privileged operation on Mac OS X.\nPlease enter your "sudo" password:'] + cmd)
    else:
        mx.run(cmd)

def inspectoragent(args):
    """launch the Inspector agent

    Launch the Inspector agent.

    The agent listens on a given port for an incoming connection from
    a remote Inspector process."""

    cmd = mx.java().format_cmd(['-cp', mx.classpath(), 'com.sun.max.tele.channel.agent.InspectorAgent'] + args)
    if mx.get_os() == 'darwin':
        # The -E option propagates the environment variables into the sudo process
        mx.run(['sudo', '-E', '-p', 'Debugging is a privileged operation on Mac OS X.\nPlease enter your "sudo" password:'] + cmd)
    else:
        mx.run(cmd)
    
def jnigen(args):
    """(re)generate Java source for native function interfaces (i.e. JNI, JMM, JVMTI)

    Run JniFunctionsGenerator.java to update the methods in [Jni|JMM|JVMTI]Functions.java
    by adding a prologue and epilogue to the @VM_ENTRY_POINT annotated methods in
    [Jni|JMM|JVMTI]FunctionsSource.java.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm'), 'com.sun.max.vm.jni.JniFunctionsGenerator'])

def jttgen(args):
    """(re)generate harness and run scheme for the JavaTester tests

    Run the JavaTester to update the JavaTesterRunScheme.java and JavaTesterTests.java
    files in the com.sun.max.vm.jtrun.all package."""


    testDirs = [join(mx.project('com.oracle.max.vm.tests').dir, 'src'), join(mx.project('com.oracle.max.tests').dir, 'src')]
    tests = []
    for testDir in testDirs:
        for name in os.listdir(join(testDir, 'jtt')):
            if name != 'hotspot' and name != 'fail':
                tests.append(join(testDir, 'jtt', name))
    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm.tests'), 'test.com.sun.max.vm.compiler.JavaTester',
                         '-scenario=target', '-run-scheme-package=all', '-native-tests'] + tests)

def loggen(args):
    """(re)generate Java source for VMLogger interfaces

    Run VMLoggerGenerator.java to update the Auto implementations of @VMLoggerInterface interfaces.

    The exit code is non-zero if a Java source file was modified."""

    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm'), 'com.sun.max.vm.log.hosted.VMLoggerGenerator'])

def makejdk(args):
    """create a JDK directory based on the Maxine VM

    Create a JDK directory by replicating the file structure of $JAVA_HOME
    and replacing the 'java' executable with the Maxine VM
    executable. This produces a Maxine VM based JDK for applications
    (such as NetBeans) which expect a certain directory structure
    and executable names in a JDK installation."""

    if mx.os == 'darwin':
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

    jdk = mx.java().jdk
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

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImageMethodTree', '-in=' + join(_vmdir, 'maxine.method.tree')] + args)

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

    mx.run_java(['-cp', mx.classpath(), 'com.sun.max.vm.hosted.BootImageObjectTree', '-in=' + join(_vmdir, 'maxine.object.tree')] + args)


def olc(args):
    """offline compile a list of methods

    See Patterns below for a description of the format expected for "patterns..."

    The output traced by this command is not guaranteed to be the same as the output
    for a compilation performed at runtime. The code produced by a compiler is sensitive
    to the compilation context such as what classes have been resolved etc.

    Use "mx olc -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    mx.run_java(['-cp', mx.classpath(), 'com.oracle.max.vm.ext.maxri.Compile'] + args)

def t1x(args):
    """alias for "mx olc -c=T1X ..." """
    olc(['-c=T1X'] + args)

def t1xgen(args):
    """(re)generate content in T1XTemplateSource.java

    Run T1XTemplateGenerator.java to generate the auto-generated templates in T1XTemplateSource.java.

    The exit code is non-zero if the auto-generated part of T1XTemplateSource.java was modified."""

    return mx.run_java(['-cp', mx.classpath('com.oracle.max.vm.ext.t1x'), 'com.oracle.max.vm.ext.t1x.T1XTemplateGenerator'])

def test(args):
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
        java = mx.java()
        mx.run_java(['-cp', mx.classpath(), 'test.com.sun.max.vm.MaxineTester', '-output-dir=maxine-tester',
                      '-refvm=' + java.java, '-refvm-args=' + ' '.join(java.java_args)] + args, out=tee.eat, err=subprocess.STDOUT)

def verify(args):
    """verifies a set of methods using the Maxine bytecode verifier

    Run the Maxine verifier over a set of specified methods available
    on the class path. To extend the class path, use one of the global
    "--cp-pfx" or "--cp-sfx" options.

    See Patterns below for a description of the format expected for "patterns..."

    Use "mx verify -help" to see what other options this command accepts.

    --- Patterns ---
    {0}"""

    mx.run_java(['-cp', mx.classpath(), 'test.com.sun.max.vm.verifier.CommandLineVerifier'] + args)
            
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
    
    mx.expand_project_in_args(args)  
    maxvmOptions = os.getenv('MAXVM_OPTIONS', '').split()
    
    debug_port = mx.java().debug_port
    if debug_port is not None:
        maxvmOptions += ['-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=' + str(debug_port)]
    
    mx.run([join(_vmdir, 'maxvm')] + maxvmOptions + args)
            
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

def _vm_image():
    return join(_vmdir, 'maxine.vm')

def mx_init():
    mx.add_argument('-V', dest='vmdir', help='directory for VM executable, shared libraries boot image and related files', metavar='<path>')
    
    commands = {
        'c1x': [c1x, '[options] patterns...'],
        'configs': [configs, ''],
        'copycheck': [copycheck, ''],
        'eclipse': [eclipse, '[VM options]'],
        'gate': [gate, '[options]'],
        'graal': [graal, '[options] patterns...'],
        'graalexample': [graalexample, '[-v] example names...'],
        'hcfdis': [hcfdis, '[options] files...'],
        'helloworld': [helloworld, '[VM options]'],
        'image': [image, '[options] classes|packages...'],
        'inspect': [inspect, '[options] [class | -jar jarfile]  [args...]'],
        'inspectoragent': [inspectoragent, '[-impl target] [-port port]'],
        'jnigen': [jnigen, ''],
        'jttgen': [jttgen, ''],
        'loggen': [loggen, ''],
        'makejdk': [makejdk, '[<destination directory>]'],
        'methodtree': [methodtree, '[options]'],
        'nm': [nm, '[options] [boot image file]', _vm_image],
        'objecttree': [objecttree, '[options]'],
        'olc': [olc, '[options] patterns...', _patternHelp],
        't1x': [t1x, '[options] patterns...'],
        't1xgen': [t1xgen, ''],
        'test': [test, '[options]'],
        'verify': [verify, '[options] patterns...', _patternHelp],
        'view': [view, '[options]'],
        'vm': [vm, '[options] [class | -jar jarfile]  [args...]']
    }
    mx.commands.update(commands)
    
def mx_post_parse_cmd_line(opts):
    global _vmdir
    if opts.vmdir is None:
        _vmdir = join(_maxine_home, 'com.oracle.max.vm.native', 'generated',  mx.get_os())
    else:
        _vmdir = opts.vmdir
