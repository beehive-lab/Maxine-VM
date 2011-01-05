/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import sun.misc.*;
import sun.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.NativeProperty;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Implements method substitutions for {@link java.lang.System java.lang.System}.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
@METHOD_SUBSTITUTIONS(System.class)
public final class JDK_java_lang_System {

    /**
     * Register any native methods through JNI.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    @ALIAS(declaringClass = System.class)
    private static PrintStream out;

    @ALIAS(declaringClass = System.class)
    private static PrintStream err;

    @ALIAS(declaringClass = System.class)
    private static InputStream in;

    /**
     * Sets the input stream {@link java.lang.System#in System.in} to the specified input stream.
     *
     * @param is the new system input stream
     */
    @SUBSTITUTE
    private static void setIn0(InputStream is) {
        in = is;
    }

    /**
     * Sets the output stream {@link java.lang.System#out System.out} to the specified output stream.
     *
     * @param ps the new system output stream
     */
    @SUBSTITUTE
    private static void setOut0(PrintStream ps) {
        out = ps;
    }

    /**
     * Sets the error stream {@link java.lang.System#err System.err} to the specified error stream.
     *
     * @param ps the new system error stream
     */
    @SUBSTITUTE
    private static void setErr0(PrintStream ps) {
        err = ps;
    }

    /**
     * Return the milliseconds elapsed since some fixed, but arbitrary time in the past.
     *
     * @return the number of milliseconds elapsed since some epoch time
     */
    @SUBSTITUTE
    public static long currentTimeMillis() {
        return MaxineVM.native_currentTimeMillis();
    }

    /**
     * Returns the nanoseconds elapsed since some fixed, but arbitrary time in the past.
     *
     * @return the number of nanoseconds elapsed since some epoch time
     */
    @SUBSTITUTE
    public static long nanoTime() {
        return MaxineVM.native_nanoTime();
    }

    /**
     * Performs an array copy in the forward direction.
     *
     * @param kind the element kind
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     * @param toComponentClassActor the class actor representing the component type of the destination array
     */
    private static void arrayCopyForward(final Kind kind, Object fromArray, int fromIndex, Object toArray, int toIndex, int length, ClassActor toComponentClassActor) {
        switch (kind.asEnum) {
            case BYTE: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setByte(toArray, toIndex + i, ArrayAccess.getByte(fromArray, fromIndex + i));
                }
                break;
            }
            case BOOLEAN: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setBoolean(toArray, toIndex + i, ArrayAccess.getBoolean(fromArray, fromIndex + i));
                }
                break;
            }
            case SHORT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setShort(toArray, toIndex + i, ArrayAccess.getShort(fromArray, fromIndex + i));
                }
                break;
            }
            case CHAR: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setChar(toArray, toIndex + i, ArrayAccess.getChar(fromArray, fromIndex + i));
                }
                break;
            }
            case INT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setInt(toArray, toIndex + i, ArrayAccess.getInt(fromArray, fromIndex + i));
                }
                break;
            }
            case FLOAT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setFloat(toArray, toIndex + i, ArrayAccess.getFloat(fromArray, fromIndex + i));
                }
                break;
            }
            case LONG: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setLong(toArray, toIndex + i, ArrayAccess.getLong(fromArray, fromIndex + i));
                }
                break;
            }
            case DOUBLE: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setDouble(toArray, toIndex + i, ArrayAccess.getDouble(fromArray, fromIndex + i));
                }
                break;
            }
            case WORD: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setWord(toArray, toIndex + i, ArrayAccess.getWord(fromArray, fromIndex + i));
                }
                break;
            }
            case REFERENCE: {
                for (int i = 0; i < length; i++) {
                    final Object object = ArrayAccess.getObject(fromArray, fromIndex + i);
                    if (toComponentClassActor != null && !toComponentClassActor.isNullOrInstance(object)) {
                        throw new ArrayStoreException();
                    }
                    ArrayAccess.setObject(toArray, toIndex + i, object);
                }
                break;
            }
            default: {
                ProgramError.unknownCase();
            }
        }
    }

    /**
     * Performs an array copy in the backward direction.
     *
     * @param kind the element kind
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     */
    private static void arrayCopyBackward(final Kind kind, Object fromArray, int fromIndex, Object toArray, int toIndex, int length) {
        switch (kind.asEnum) {
            case BYTE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setByte(toArray, toIndex + i, ArrayAccess.getByte(fromArray, fromIndex + i));
                }
                break;
            }
            case BOOLEAN: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setBoolean(toArray, toIndex + i, ArrayAccess.getBoolean(fromArray, fromIndex + i));
                }
                break;
            }
            case SHORT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setShort(toArray, toIndex + i, ArrayAccess.getShort(fromArray, fromIndex + i));
                }
                break;
            }
            case CHAR: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setChar(toArray, toIndex + i, ArrayAccess.getChar(fromArray, fromIndex + i));
                }
                break;
            }
            case INT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setInt(toArray, toIndex + i, ArrayAccess.getInt(fromArray, fromIndex + i));
                }
                break;
            }
            case FLOAT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setFloat(toArray, toIndex + i, ArrayAccess.getFloat(fromArray, fromIndex + i));
                }
                break;
            }
            case LONG: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setLong(toArray, toIndex + i, ArrayAccess.getLong(fromArray, fromIndex + i));
                }
                break;
            }
            case DOUBLE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setDouble(toArray, toIndex + i, ArrayAccess.getDouble(fromArray, fromIndex + i));
                }
                break;
            }
            case WORD: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setWord(toArray, toIndex + i, ArrayAccess.getWord(fromArray, fromIndex + i));
                }
                break;
            }
            case REFERENCE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setObject(toArray, toIndex + i, ArrayAccess.getObject(fromArray, fromIndex + i));
                }
                break;
            }
            default: {
                ProgramError.unknownCase();
            }
        }
    }

    /**
     * Copies a portion of an array from one array to another (possibly the same) array.
     *
     * @see java.lang.System#arraycopy(Object, int, Object, int, int)
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     */
    @SUBSTITUTE
    public static void arraycopy(Object fromArray, int fromIndex, Object toArray, int toIndex, int length) {
        if (fromArray == null || toArray == null) {
            throw new NullPointerException();
        }
        final Hub fromHub = ObjectAccess.readHub(fromArray);
        final ClassActor fromArrayClassActor = fromHub.classActor;
        if (!fromArrayClassActor.isArrayClass()) {
            throw new ArrayStoreException();
        }
        final Kind kind = fromArrayClassActor.componentClassActor().kind;
        if (fromArray == toArray) {
            if (fromIndex < toIndex) {
                if (fromIndex < 0 || length < 0 || toIndex + length > ArrayAccess.readArrayLength(fromArray)) {
                    throw new IndexOutOfBoundsException();
                }
                arrayCopyBackward(kind, fromArray, fromIndex, fromArray, toIndex, length);
            } else if (fromIndex != toIndex) {
                if (toIndex < 0 || length < 0 || fromIndex + length > ArrayAccess.readArrayLength(fromArray)) {
                    throw new IndexOutOfBoundsException();
                }
                arrayCopyForward(kind, fromArray, fromIndex, fromArray, toIndex, length, null);
            }
            return;
        }
        final Hub toHub = ObjectAccess.readHub(toArray);
        if (toHub == fromHub) {
            if (fromIndex < 0 || toIndex < 0 || length < 0 ||
                    fromIndex + length > ArrayAccess.readArrayLength(fromArray) ||
                    toIndex + length > ArrayAccess.readArrayLength(toArray)) {
                throw new IndexOutOfBoundsException();
            }
            arrayCopyForward(kind, fromArray, fromIndex, toArray, toIndex, length, null);
        } else {
            final ClassActor toArrayClassActor = toHub.classActor;
            if (!toArrayClassActor.isArrayClass()) {
                throw new ArrayStoreException();
            }
            final ClassActor toComponentClassActor = toArrayClassActor.componentClassActor();
            if (kind != Kind.REFERENCE || toComponentClassActor.kind != Kind.REFERENCE) {
                throw new ArrayStoreException();
            }
            if (fromIndex < 0 || toIndex < 0 || length < 0 ||
                    fromIndex + length > ArrayAccess.readArrayLength(fromArray) ||
                    toIndex + length > ArrayAccess.readArrayLength(toArray)) {
                throw new IndexOutOfBoundsException();
            }
            arrayCopyForward(kind, fromArray, fromIndex, toArray, toIndex, length, toComponentClassActor);
        }
    }

    /**
     * Gets the default (identity) hashcode for an object.
     *
     * @see java.lang.System#identityHashCode(Object)
     * @param object the object for which to get the hashcode
     * @return the identity hashcode of the specified object; {@code 0} if the object reference is null
     */
    @SUBSTITUTE
    public static int identityHashCode(Object object) {
        if (object == null) {
            return 0;
        }
        return ObjectAccess.makeHashCode(object);
    }

    /**
     * Sets a property in the specified property set, if it has not already been set.
     *
     * @param properties the properties set in which to set the property
     * @param name the name of the property
     * @param value the value to which the property will be set if it doesn't already have a value
     */
    private static void setIfAbsent(Properties properties, String name, String value) {
        if (properties.containsKey(name)) {
            return; // property has already been set by command line argument parsing
        }
        if (value != null) {
            properties.setProperty(name, value);
        }
    }

    /**
     * Finds the Java home path, where the JDK libraries are found.
     *
     * @return a string representing the path of the Java home
     */
    private static String findJavaHome() {
        switch (platform().os) {
            case MAXVE:
            case SOLARIS:
            case LINUX: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                String javaHome = getenv("JAVA_HOME", false);
                FatalError.check(javaHome != null, "Environment variable JAVA_HOME not set");

                if (!javaHome.endsWith("/jre")) {
                    // A lot of JDK code expects java.home to point to the "jre" subdirectory in a JDK.
                    // This makes interpreting java.home easier if only a JRE is installed.
                    // However, quite often JAVA_HOME points to the top level directory of a JDK
                    // installation and so it needs to be adjusted to append the "jre" subdirectory.
                    javaHome = javaHome + "/jre";
                }
                return javaHome;
            }
            case WINDOWS: {
                throw FatalError.unexpected("Initialization of java.home is unimplemented");
            }
            case DARWIN: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                String javaHome = getenv("JAVA_HOME", false);
                if (javaHome == null) {
                    javaHome = "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home";
                }

                if (!javaHome.endsWith("/Home")) {
                    javaHome = javaHome + "/Home";
                }
                return javaHome;
            }
        }
        throw FatalError.unexpected("should not reach here");
    }

    /**
     * Retrieves the java library path from OS-specific environment variable(s).
     *
     * @return a string representing the java library path as determined from the OS environment
     */
    private static String getenvJavaLibraryPath() {
        switch (platform().os) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("LD_LIBRARY_PATH", false);
            }
            case WINDOWS:
            case MAXVE:
            default: {
                return "";
            }
        }
    }

    /**
     * Retrieves the java class path from OS-specified environment variable(s). This method
     * does not consult the value of the command line arguments to the virtual machine.
     *
     * @return a string representing the Java class path as determined from the OS environment
     */
    private static String getenvClassPath() {
        switch (platform().os) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("CLASSPATH", false);
            }
            case WINDOWS:
            case MAXVE:
            default: {
                return "";
            }
        }
    }

    private static String getenv(String name, boolean mustBePresent) {
        final String value = System.getenv(name);
        if (mustBePresent && value == null) {
            FatalError.unexpected("Required environment variable " + name + " is undefined.");
        }
        return value;
    }

    /**
     * Retrieves the path to the executable binary which launched this virtual machine, which is
     * used to derived the java home, bootstrap classpath, etc.
     *
     * @return a path representing the location of the virtual machine executable, as determined
     * by the OS environment
     */
    private static String getenvExecutablePath() {
        final String executablePath = MaxineVM.getExecutablePath();
        if (executablePath == null) {
            FatalError.unexpected("Path to VM executable cannot be null.");
        }
        return executablePath;
    }

    /**
     * Gets the name of this VM's target instruction set, which is used to locate
     * platform-specific native libraries.
     *
     * @return the name of this VM's target ISA
     */
    private static String getISA() {
        switch (Platform.platform().isa) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == 64) ? "sparcv9" : "sparc";
            case PPC:
                FatalError.unimplemented();
                break;
        }
        return null;
    }

    /**
     * Gets a list of this VM's target instruction sets.
     * @return a list of this VM's target ISAs
     */
    private static String getISAList() {
        switch (Platform.platform().isa) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == 64) ? "sparcv9" : "sparc";
            case PPC:
                FatalError.unimplemented();
                break;
        }
        return null;
    }

    static String fileSeparator;

    static String pathSeparator;

    /**
     * Joins an array of strings into a single string.
     *
     * @param separator the separator string
     * @param strings an array of strings to be joined. Any element of this array that is null or has a length of zero
     *            is ignored.
     * @return the result of joining all the non-null and non-empty values in {@code strings} as a single string with
     *         {@code separator} inserted between each pair of strings
     */
    private static String join(String separator, String... strings) {
        int length = 0;
        for (String path : strings) {
            if (path != null && !path.isEmpty()) {
                length += path.length() + separator.length();
            }
        }
        final StringBuilder result = new StringBuilder(length);
        for (String path : strings) {
            if (path != null && !path.isEmpty()) {
                if (result.length() != 0) {
                    result.append(separator);
                }
                result.append(path);
            }
        }
        return result.toString();
    }

    private static String asFilesystemPath(String... directoryNames) {
        return join(fileSeparator, directoryNames);
    }

    private static String asClasspath(String... filesystemPaths) {
        return join(pathSeparator, filesystemPaths);
    }

    private static final VMStringOption classpathOption = register(new VMStringOption("-cp", true, null,
        "A " + File.pathSeparatorChar + " separated list of directories, JAR archives, and ZIP archives to search for class files.") {
        @Override
        public boolean matches(Pointer arg) {
            return CString.equals(arg, prefix) || CString.equals(arg, "-classpath");
        }
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(category(), "-cp", " <class search path of directories and zip/jar files>", null);
            VMOptions.printHelpForOption(category(), "-classpath",
                " <class search path of directories and zip/jar files>", help);
        }
    }, MaxineVM.Phase.PRISTINE);

    private static final BootClasspathVMOption bootClasspathOption = BootClasspathVMOption.create(":", "set search path for bootstrap classes and resources.");
    private static final BootClasspathVMOption aBootClasspathOption = BootClasspathVMOption.create("/a:", "append to end of bootstrap class path");
    private static final BootClasspathVMOption pBootClasspathOption = BootClasspathVMOption.create("/p:", "prepend in front of bootstrap class path");

    static class BootClasspathVMOption extends VMOption {
        private String path;
        private static final String USAGE_VALUE = "<directories and zip/jar files separated by " + File.pathSeparatorChar + ">";

        @HOSTED_ONLY
        static BootClasspathVMOption create(String suffix, String help) {
            return register(new BootClasspathVMOption(suffix, help), MaxineVM.Phase.STARTING);
        }

        @HOSTED_ONLY
        BootClasspathVMOption(String suffix, String help) {
            super("-Xbootclasspath" + suffix, help);
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            try {
                path = CString.utf8ToJava(optionValue);
                return true;
            } catch (Utf8Exception utf8Exception) {
                return false;
            }
        }

        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(category(), prefix, USAGE_VALUE, help);
        }

        String path() {
            return path;
        }
    }

    // Checkstyle: stop method name check
    @ALIAS(declaringClassName = "java.lang.ProcessEnvironment", name = "<clinit>")
    private static native void ProcessEnvironment_clinit();

    @ALIAS(declaringClassName = "java.lang.ApplicationShutdownHooks", name = "<clinit>")
    private static native void ApplicationShutdownHooks_clinit();

    @ALIAS(declaringClass = File.class, name = "<clinit>")
    private static native void File_clinit();

    @ALIAS(declaringClass = Perf.class, name = "<clinit>")
    private static native void Perf_clinit();

    // Checkstyle: resume method name check

    /**
     * Initializes system properties from a wide variety of sources.
     */
    @SUBSTITUTE
    private static Properties initProperties(Properties properties) {
        // 1. parse any properties from command line
        VMOptions.addParsedSystemProperties(properties);

        // 2. set up basic Maxine configuration information
        setIfAbsent(properties, "java.runtime.name", MaxineVM.name());
        setIfAbsent(properties, "java.runtime.version", MaxineVM.VERSION_STRING);

        setIfAbsent(properties, "java.vm.name", MaxineVM.name());
        setIfAbsent(properties, "java.vm.version", MaxineVM.VERSION_STRING);
        setIfAbsent(properties, "java.vm.info", vmConfig().compilationScheme().mode().name().toLowerCase() + " mode");

        setIfAbsent(properties, "sun.arch.data.model", Integer.toString(Word.width()));
        setIfAbsent(properties, "sun.cpu.endian", Word.endianness().name().toLowerCase());

        switch (Platform.platform().endianness()) {
            case LITTLE:
                setIfAbsent(properties, "sun.io.unicode.encoding", "UnicodeLittle");
                break;
            case BIG:
                setIfAbsent(properties, "sun.io.unicode.encoding", "UnicodeBig");
                break;
        }

        String isa = properties.getProperty("os.arch");
        if (isa == null) {
            isa = getISA();
            setIfAbsent(properties, "os.arch", isa);
        }

        String isaList = properties.getProperty("sun.cpu.isalist");
        if (isaList == null) {
            isaList = getISAList(); // TODO: reconcile with code below
            setIfAbsent(properties, "sun.cpu.isalist", isaList);
        }

        // 3. reinitialize java.lang.ProcessEnvironment with this process's environment
        ProcessEnvironment_clinit();

        // 3.1. reinitialize java.lang.ApplicationShutdownHooks
        ApplicationShutdownHooks_clinit();

        // 4. perform OS-specific initialization
        switch (Platform.platform().os) {
            case DARWIN:
                setIfAbsent(properties, "os.name", "Mac OS X");
                initBasicUnixProperties(properties);
                break;
            case MAXVE:
                setIfAbsent(properties, "os.name", "Maxine VE");
                setIfAbsent(properties, "java.io.tmpdir", "/tmp");
                initBasicUnixProperties(properties);
                break;
            case LINUX:
                setIfAbsent(properties, "os.name", "Linux");
                initBasicUnixProperties(properties);
                break;
            case SOLARIS:
                setIfAbsent(properties, "os.name", "SunOS");
                initBasicUnixProperties(properties);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }

        // 5. set up user-specific information
        final Pointer nativeJavaProperties = MaxineVM.native_properties();
        final String userDir = NativeProperty.USER_DIR.value(nativeJavaProperties);
        if (userDir == null) {
            throw FatalError.unexpected("Could not determine current working directory.");
        }

        setIfAbsent(properties, "user.name", NativeProperty.USER_NAME.value(nativeJavaProperties));
        setIfAbsent(properties, "user.home", NativeProperty.USER_HOME.value(nativeJavaProperties));
        setIfAbsent(properties, "user.dir", userDir);
        setIfAbsent(properties, "java.java2d.fontpath", getenv("JAVA2D_FONTPATH", false));

        // 6. set up the java home
        String javaHome = properties.getProperty("java.home");
        if (javaHome == null) {
            javaHome = findJavaHome();
            setIfAbsent(properties, "java.home", javaHome);
        }

        // 7. set up classpath and library path
        final String[] javaAndZipLibraryPaths = new String[2];
        if (Platform.platform().os == OS.DARWIN) {
            initDarwinPathProperties(properties, javaHome, javaAndZipLibraryPaths);
        } else if (Platform.platform().os == OS.WINDOWS) {
            initWindowsPathProperties(properties, javaHome, javaAndZipLibraryPaths);
        } else {
            initUnixPathProperties(properties, javaHome, isa, javaAndZipLibraryPaths);
        }
        setIfAbsent(properties, "java.library.path", getenvJavaLibraryPath());

        // 8. set up the class path
        // N.B. -jar overrides any other classpath setting
        if (VMOptions.jarFile() == null) {
            String javaClassPath = classpathOption.getValue();
            if (javaClassPath == null) {
                javaClassPath = getenvClassPath();
            }
            setIfAbsent(properties, "java.class.path", javaClassPath);
        } else {
            properties.put("java.class.path", VMOptions.jarFile());
        }

        // 9. load the native code for zip and java libraries
        BootClassLoader.BOOT_CLASS_LOADER.loadJavaAndZipNativeLibraries(javaAndZipLibraryPaths[0], javaAndZipLibraryPaths[1]);

        // 10. initialize the file system with current runtime values as opposed to bootstrapping values
        File_clinit();

        // 11. initialize the management performance class with current runtime values
        Perf_clinit();

        // 12. load the character encoding class
        final String sunJnuEncodingValue = properties.getProperty("sun.jnu.encoding");
        properties.remove("sun.jnu.encoding"); // Avoids endless recursion in the next statement
        Charset.isSupported(sunJnuEncodingValue); // We are only interested in the side effect: loading the char set if supported and initializing related JNU variables
        setIfAbsent(properties, "sun.jnu.encoding", sunJnuEncodingValue); // Now that we have loaded the char set, the recursion is broken and we can move on

        if (verboseOption.verboseProperties) {
            Log.println("Initial system properties:");
            final Map<String, String> sortedProperties = new TreeMap<String, String>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                sortedProperties.put((String) entry.getKey(), (String) entry.getValue());
            }
            for (Map.Entry<String, String> entry : sortedProperties.entrySet()) {
                Log.print(entry.getKey());
                Log.print('=');
                Log.println(entry.getValue());
            }
        }

        return properties;
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path and java.ext.dirs system properties when running on Unix.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the libjava.jnilib will be returned in
     *            element 0 and the path to libzip.jnilib will be returned in element 1
     */
    @PLATFORM(os = "!(windows|darwin)")
    private static void initUnixPathProperties(Properties properties, String javaHome, String isa, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/jre"), "The java.home system property should end with \"/jre\"");
        final String jrePath = javaHome;
        final String jreLibPath = asFilesystemPath(jrePath, "lib");
        final String jreLibIsaPath = asFilesystemPath(jreLibPath, isa);

        setIfAbsent(properties, "sun.boot.library.path", asClasspath(getenvExecutablePath(), jreLibIsaPath));

        String bootClassPath = null;
        if (bootClasspathOption.isPresent()) {
            bootClassPath = bootClasspathOption.path();
        } else {
            bootClassPath = join(pathSeparator,
                            asFilesystemPath(jreLibPath, "resources.jar"),
                            asFilesystemPath(jreLibPath, "rt.jar"),
                            asFilesystemPath(jreLibPath, "sunrsasign.jar"),
                            asFilesystemPath(jreLibPath, "jsse.jar"),
                            asFilesystemPath(jreLibPath, "jce.jar"),
                            asFilesystemPath(jreLibPath, "charsets.jar"),
                            asFilesystemPath(jrePath, "classes"));
        }
        setIfAbsent(properties, "sun.boot.class.path", checkAugmentBootClasspath(bootClassPath));

        javaAndZipLibraryPaths[0] = jreLibIsaPath;
        javaAndZipLibraryPaths[1] = jreLibIsaPath;

        final OS os = Platform.platform().os;
        if (os == OS.LINUX) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext"), "/usr/java/packages/lib/ext"));
        } else if (os == OS.SOLARIS) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext"), "/usr/jdk/packages/lib/ext"));
        } else if (os == OS.MAXVE) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext")));
        } else {
            ProgramError.unknownCase(os.toString());
        }
    }

    static String checkAugmentBootClasspath(final String xBootClassPath) {
        String bootClassPath = xBootClassPath;
        if (aBootClasspathOption.isPresent()) {
            bootClassPath = join(pathSeparator, bootClassPath, aBootClasspathOption.path());
        }
        if (pBootClasspathOption.isPresent()) {
            bootClassPath = join(pathSeparator, pBootClasspathOption.path(), bootClassPath);
        }
        return bootClassPath;
    }

    /**
     * Initializes the sun.boot.library.path and sun.boot.path system properties when running on Windows.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the java.dll will be returned in
     *            element 0 and the path to zip.dll will be returned in element 1
     */
    @PLATFORM(os = "windows")
    private static void initWindowsPathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        throw FatalError.unexpected("Initialization of paths on Windows is unimplemented");
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path and java.ext.dirs system properties when running on Darwin.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the {@code libjava.jnilib} will be returned in
     *            element 0 and the path to {@code libzip.jnilib} will be returned in element 1
     */
    @PLATFORM(os = "darwin")
    private static void initDarwinPathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/Home"), "The java.home system property should end with \"/Home\"");
        final String javaPath = Strings.chopSuffix(javaHome, "/Home");

        final String librariesPath = javaPath + "/Libraries";
        setIfAbsent(properties, "sun.boot.library.path", asClasspath(getenvExecutablePath(), librariesPath));

        final String classesPath = javaPath + "/Classes";
        String bootClassPath = null;
        if (bootClasspathOption.isPresent()) {
            bootClassPath = bootClasspathOption.path();
        } else {
            bootClassPath = join(pathSeparator,
                        asFilesystemPath(classesPath, "classes.jar"),
                        asFilesystemPath(classesPath, "ui.jar"),
                        asFilesystemPath(classesPath, "laf.jar"),
                        asFilesystemPath(classesPath, "sunrsasign.jar"),
                        asFilesystemPath(classesPath, "jsse.jar"),
                        asFilesystemPath(classesPath, "jce.jar"),
                        asFilesystemPath(classesPath, "charsets.jar"));
        }
        setIfAbsent(properties, "sun.boot.class.path", checkAugmentBootClasspath(bootClassPath));
        javaAndZipLibraryPaths[0] = librariesPath;
        javaAndZipLibraryPaths[1] = librariesPath;

        String extDirs = "/Library/Java/Extensions:/System/Library/Java/Extensions:" + javaHome + "/lib/ext";
        final String userHome = properties.getProperty("user.home");
        if (userHome != null) {
            final File userExtDir = new File(userHome, "Library/Java/Extensions/");
            extDirs += ":" + userExtDir;
        }
        setIfAbsent(properties, "java.ext.dirs", extDirs);
    }

    @PLATFORM(os = "!windows")
    private static void initBasicUnixProperties(Properties properties) {
        setIfAbsent(properties, "java.io.tmpdir", "/var/tmp");
        setIfAbsent(properties, "java.awt.printerjob", "sun.print.PSPrinterJob");
        setIfAbsent(properties, "os.version", ""); // TODO
        setIfAbsent(properties, "sun.os.patch.level", "unknown");
        setIfAbsent(properties, "java.awt.graphicsenv", "sun.awt.X11GraphicsEnvironment");
        setIfAbsent(properties, "file.encoding", "UTF-8");
        setIfAbsent(properties, "sun.jnu.encoding", "UTF-8");

        if (getenv("GNOME_DESKTOP_SESSION_ID", false) != null) {
            setIfAbsent(properties, "sun.desktop", "gnome");
        }
        setIfAbsent(properties, "user.language", "en"); // TODO
        setIfAbsent(properties, "user.country", "US"); // TODO
        setIfAbsent(properties, "user.variant", ""); // TODO

        fileSeparator = "/";
        pathSeparator = ":";
        setIfAbsent(properties, "line.separator", "\n");
        setIfAbsent(properties, "file.separator", fileSeparator);
        setIfAbsent(properties, "path.separator", pathSeparator);
    }

    /**
     * Converts a simple library name to a platform-specific name with either or both
     * a prefix and a suffix.
     *
     * @param libraryName the name of the library
     * @return a string representing the traditional name of a native library on this platform
     */
    @SUBSTITUTE
    public static String mapLibraryName(String libraryName) {
        if (libraryName == null) {
            throw new NullPointerException();
        }
        switch (platform().os) {
            case DARWIN:
                // System.loadLibrary() first wants to look for a library with the extension ".jnilib",
                // then if the library was not found, try again with extension ".dylib".
                // We support this by returning its first choice here:
                return "lib" + libraryName + ".jnilib";
            case LINUX:
            case MAXVE:
            case SOLARIS:
                return "lib" + libraryName + ".so";
            case WINDOWS:
                return libraryName + ".dll";
            default:
                throw ProgramError.unknownCase();
        }
    }

    @ALIAS(declaringClass = Runtime.class)
    private native void loadLibrary0(Class fromClass, String libname);

    @INTRINSIC(UNSAFE_CAST)
    private static native JDK_java_lang_System asThis(Runtime runtime);

    /**
     * Loads a native library, searching the library paths as necessary.
     *
     * @param name the name of the library to load
     * @see BootClassLoader#loadJavaAndZipNativeLibraries(String, String)
     */
    @SUBSTITUTE
    @NEVER_INLINE
    public static void loadLibrary(String name) throws Throwable {
        if (name.equals("zip")) {
            // Do nothing, since we already loaded this library ahead of time
            // to avoid bootstrap issues with class loading (and thus dynamic constant pool resolution).
        } else {
            // ATTENTION: these statements must have the exact same side effects as the original code of the substitutee:
            final Class callerClass = Reflection.getCallerClass(2);

            Runtime runtime = Runtime.getRuntime();
            asThis(runtime).loadLibrary0(callerClass, name);
        }

        // This has been added to re-initialize those classes in the boot image that had to wait for this library to appear:
        vmConfig().runScheme().runNativeInitializationMethods();
    }
}
